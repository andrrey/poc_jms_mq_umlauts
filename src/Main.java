import javax.annotation.Resource;
import javax.jms.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

public class Main extends javax.servlet.http.HttpServlet {
    @Resource(lookup = "jms/BlsQCF")   private static ConnectionFactory connectionFactory;
    @Resource(lookup = "jms/pocQueue") private static Queue queue;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        out.print("<!DOCTYPE html>\n");
        out.print("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=ISO-8859-1\">");
        out.print("<html>\n" +
                "<head>\n" +
                "<title>Test Umlauts in MQ using JMS</title>\n" +
                "</head>\n" +
                "<body>");

        Connection connection = null;
        MessageConsumer consumer;
        try {
            String fileName = request.getParameter("file");
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage();

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "ISO-8859-1"));
            StringBuilder sb = new StringBuilder();

            out.print("Read from file:<br />");

            for(String s = ""; s!=null; s=br.readLine()) {
                printStrln(s, out);
                sb.append(s);
                sb.append("\n");
            }

            message.setText(sb.toString());
            producer.send(message);

            //And now receive message back
            consumer = session.createConsumer(queue);
            connection.start(); //this will start connection => start delivery of message
            Message m = consumer.receive(1);

            if (m instanceof TextMessage) {
                message = (TextMessage) m;
                out.print("TextMessage received<br />");
                out.print("Reading message with message.getText():<br />");
                printStrln(message.getText(), out);
                StreamSource source = new StreamSource(new ByteArrayInputStream(message.getText().getBytes("UTF-8")));

                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer();
                transformer.transform(source, result);
                String strResult = writer.toString();

                out.print("Reading and decoding message with message.getText().getBytes(\"UTF-8\"):<br />");
                printStrln(strResult, out);
            } else {
                out.print("NOT a TextMessage received!<br />");
            }

        } catch (Exception e) {
            e.printStackTrace(out);
        }finally {
            if (connection != null) {
                try { connection.close(); }
                catch (JMSException e) { }
            }
        }

        out.print("</body>\n" + "</html>");
        out.close();
    }

    private void printStr (String str, PrintWriter wrt){
        String s=str.replaceAll("<", "&lt;");
        s=s.replaceAll(">", "&gt;");
        wrt.print(s);
    }

    private void printStrln (String str, PrintWriter wrt){
        printStr(str,wrt);
        wrt.print("<br />");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
