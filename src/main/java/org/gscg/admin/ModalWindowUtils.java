package org.gscg.admin;

public class ModalWindowUtils
{

    public static void popup(final AdminData data, final String title, final String message, final String okMethod)
    {
        // make popup
        // System.err.println("popup with title [" + title + "] and message: " + message);
        StringBuilder s = new StringBuilder();
        s.append("<p>");
        s.append(message);
        s.append("</p>\n");
        data.setModalWindowHtml(makeOkModalWindow(title, s.toString(), okMethod));
        data.setShowModalWindow(true);
    }

    public static String makeModalWindow(final String title, final String content, final String onClickClose)
    {
        StringBuilder s = new StringBuilder();
        s.append("    <div class=\"gscg-modal\">\n");
        s.append("      <div class=\"gscg-modal-window\" id=\"gscg-modal-window\">\n");
        s.append("        <div class=\"gscg-modal-window-header\">");
        s.append("          <span class=\"gscg-modal-close\" onclick=\"");
        s.append(onClickClose);
        s.append("\">");
        s.append("&times;</span>\n");
        s.append("          <p>");
        s.append(title);
        s.append("</p>\n");
        s.append("        </div>\n");
        s.append(content);
        s.append("      </div>\n");
        s.append("    </div>\n");
        s.append("    <script>");
        s.append("      dragElement(document.getElementById(\"gscg-modal-window\"));");
        s.append("    </script>");
        return s.toString();
    }

    public static String makeOkModalWindow(final String title, final String htmlText, final String okMethod)
    {
        StringBuilder s = new StringBuilder();
        s.append("        <div class=\"gscg-modal-body\">");
        s.append("          <div class=\"gscg-modal-text\">\n");
        s.append("            <p>\n");
        s.append(htmlText);
        s.append("            </p>\n");
        s.append("          <div class=\"gscg-modal-button-row\">\n");
        s.append("            <div class=\"gscg-button-small\" onclick=\"" + okMethod + "\">OK</div>\n");
        s.append("          </div>\n");
        s.append("        </div>\n");
        return makeModalWindow(title, s.toString(), okMethod);
    }

    public static String makeOkModalWindow(final String title, final String htmlText)
    {
        return makeOkModalWindow(title, htmlText, "clickModalWindowOk()");
    }

    public static void make2ButtonModalWindow(final AdminData data, final String title, final String content, final String buttonText1,
            final String buttonMethod1, final String buttonText2, final String buttonMethod2, final String closeMethod)
    {
        StringBuilder s = new StringBuilder();
        s.append("    <div class=\"gscg-modal\">\n");
        s.append("      <div class=\"gscg-modal-window\" id=\"gscg-modal-window\">\n");
        s.append("        <div class=\"gscg-modal-window-header\">");
        s.append("          <span class=\"gscg-modal-close\" onclick=\"");
        s.append(closeMethod);
        s.append("\">");
        s.append("&times;</span>\n");
        s.append("          <p>");
        s.append(title);
        s.append("</p>\n");
        s.append("        </div>\n"); // gscg-modal-window-header
        s.append("        <div class=\"gscg-modal-body\">");
        s.append("          <div class=\"gscg-modal-text\">\n");
        s.append("            <p>\n");
        s.append(content);
        s.append("            </p>\n");
        s.append("          </div>\n"); // gscg-modal-text
        s.append("          <div class=\"gscg-modal-button-row\">\n");
        s.append("            <div class=\"gscg-button-small\" onclick=\"");
        s.append(buttonMethod1);
        s.append("\">");
        s.append(buttonText1);
        s.append("</div>\n");
        s.append("            <div class=\"gscg-button-small\" onclick=\"");
        s.append(buttonMethod2);
        s.append("\">");
        s.append(buttonText2);
        s.append("</div>\n");
        s.append("          </div>\n"); // gscg-modal-button-row
        s.append("        </div>\n"); // gscg-gscg-modal-body
        s.append("      </div>\n"); // gscg-modal-window
        s.append("    </div>\n"); // gscg-modal
        s.append("    <script>");
        s.append("      dragElement(document.getElementById(\"gscg-modal-window\"));");
        s.append("    </script>");

        data.setModalWindowHtml(s.toString());
        data.setShowModalWindow(true);
    }

}
