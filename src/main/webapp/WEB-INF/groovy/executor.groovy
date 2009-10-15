import org.codehaus.groovy.control.MultipleCompilationErrorsException

import com.google.apphosting.api.ApiProxy

import spockwebconsole.ScriptRunner
import spockwebconsole.ScriptOutput

def scriptText = request.getParameter("script") ?: "'The received script was null.'"

def result = ""
def output = new ByteArrayOutputStream()
def stacktrace = new StringWriter()

ScriptOutput.redirectTo(output) {
  def env = ApiProxy.getCurrentEnvironment()
  ApiProxy.clearEnvironmentForCurrentThread()
  try {
    result = new ScriptRunner().run(scriptText)
  } catch (MultipleCompilationErrorsException e) {
    stacktrace.append(e.message - 'startup failed, Script1.groovy: ')
  } catch (Throwable t) {
    sanitizeStacktrace(t)
    def cause = t
    while (cause = cause?.cause) {
      sanitizeStacktrace(cause)
    }
    t.printStackTrace(new PrintWriter(stacktrace))
  } finally {
    ApiProxy.setEnvironmentForCurrentThread(env)
  }
}

response.contentType = "application/json"

out.println """{
	executionResult: "${escape(result)}",
 	outputText: "${escape(output.toString('utf-8'))}",
 	stacktraceText: "${escape(stacktrace)}"
}"""

def escape(object) {
	object.toString().replaceAll(/\n/, /\\\n/).replaceAll(/"/, /\\"/)
}

def sanitizeStacktrace(t) {
  def filtered = [
    'com.google.', 'org.mortbay.',
    'java.', 'javax.', 'sun.', 
    'groovy.', 'org.codehaus.groovy.',
    'groovyx.gaelyk.', 'executor'
	]
  def trace = t.getStackTrace()
  def newTrace = []
  trace.each { stackTraceElement -> 
    if (filtered.every { !stackTraceElement.className.startsWith(it) }) {
      newTrace << stackTraceElement
    }
  }
  def clean = newTrace.toArray(newTrace as StackTraceElement[])
  t.stackTrace = clean
}