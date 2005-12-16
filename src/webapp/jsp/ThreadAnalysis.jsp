<%@ page import="java.lang.management.*" %>
<%@ page import="java.util.*" %>
<html>
<head>
  <title>JVM Thread Monitor</title>
</head>

<%
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
%>
        <table border="0" width="100%">
        <tr><td align="center"><h3>Thread MXBean</h3></td></tr>

        <tr><td align="center"><h4>Deadlocks</h4></td></tr>

<%
        try
        {
                long ids[] = t.findMonitorDeadlockedThreads();
                if (ids == null || ids.length == 0)
                {
                        out.println("<tr><td align=\"center\">None</td></tr>");
                }
                else
                {
                        for (int n = 0; n < ids.length; n++)
                        {
                                if (n != 0)
                                        out.print(", ");
                                out.print(ids[n] + "<br/>");
                        }
                }
        }
        catch (Exception e)
        {
                out.println("[call to findMonitorDeadlockedThreads failed: " + e + "]");
        }
%>
        <tr><td align="center"><h4>All Threads</h4></td></tr>
<%
        long threads[] = t.getAllThreadIds();
        ThreadInfo[] tinfo = t.getThreadInfo(threads, 15);

        for (int i=0; i<tinfo.length; i++)
        {
                %>
                <tr><td align="center">
                <%
                ThreadInfo e = tinfo[i];

                StackTraceElement[] el = e.getStackTrace();
                out.print("<br/>" + e.getThreadName() + "<br/>" + " " + " Thread id = " + e.getThreadId() + " " + e.getThreadState());

                if (el == null || el.length == 0)
                {
                        out.print("&nbsp;&nbsp;&nbsp;&nbsp;no stack trace available");
                        return;
               }

                for (int n = 0; n < el.length; n++)
                {
                        if (n != 0)
                                out.print("<br/>");

                        StackTraceElement frame = el[n];

                        if (frame == null) {
                                out.print("&nbsp;&nbsp;&nbsp;&nbsp;null stack frame");
                                continue;
                        }

                        out.print("&nbsp;&nbsp;&nbsp;&nbsp;");
                        out.print(frame.toString());
                }
                %>
                </td></tr>
                <%
        }
%>

</table>
                        