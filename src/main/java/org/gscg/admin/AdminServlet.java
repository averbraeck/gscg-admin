package org.gscg.admin;

import java.io.IOException;

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.gscg.admin.Menus.SubMenu;
import org.gscg.admin.form.table.TableForm;
import org.gscg.admin.table.TableUser;

@WebServlet("/admin")
@MultipartConfig
public class AdminServlet extends HttpServlet
{

    /** */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();

        AdminData data = SessionUtils.getData(session);
        if (data == null)
        {
            response.sendRedirect("/gscg-admin/login");
            return;
        }

        String click = "";
        if (request.getParameter("click") != null)
            click = request.getParameter("click").toString();
        else if (request.getParameter("editClick") != null)
            click = request.getParameter("editClick").toString();

        int recordId = 0;
        if (request.getParameter("recordId") != null)
            recordId = Integer.parseInt(request.getParameter("recordId"));
        else if (request.getParameter("editRecordId") != null)
            recordId = Integer.parseInt(request.getParameter("editRecordId"));

        System.out.println("Clicked: " + click);

        data.setShowModalWindow(false);
        data.setModalWindowHtml("");

        // state machine
        if (click.equals("menu-logoff"))
        {
            response.sendRedirect("jsp/admin/login.jsp");
            return;
        }

        if (click.equals("menu-admin-panel") || click.equals("menu-home"))
            handleHome(request, response, click, data);
        else if (click.equals("menu-settings"))
            handleSettings(request, response, click, data);
        else if (click.startsWith("menu"))
            handleMenu(request, response, click, data, recordId);
        else if (click.startsWith("submenu"))
            handleSubMenu(request, response, click, data, recordId);
        else if (click.equals("record-new") || click.equals("record-view") || click.equals("record-edit"))
            handleRecordEdit(request, response, click, data, recordId);
        else if (click.equals("record-save"))
            handleRecordSave(request, response, click, data, recordId);
        else if (click.equals("record-cancel"))
            handleRecordCancel(request, response, click, data, recordId);
        else if (click.equals("record-ok"))
            handleRecordOk(request, response, click, data, recordId);
        else if (click.equals("record-reedit"))
            handleRecordReEdit(request, response, click, data, recordId);
        else if (click.equals("record-delete"))
            handleRecordDelete(request, response, click, data, recordId);
        else if (click.equals("record-delete-ok"))
            handleRecordDeleteOk(request, response, click, data, recordId);
        // else if (click.equals("record-select"))
        // handleRecordSelect(request, response, click, data, recordId);
        else if (click.startsWith("close-"))
            handleCloseSelect(request, response, click, data, recordId);
        else if (click.startsWith("az"))
            handleSort(request, response, click, data, recordId);
        else
            System.err.println("Unknown menu choice: " + click + " with recordId: " + recordId);

        response.sendRedirect("jsp/admin/admin.jsp");
    }

    private void handleHome(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data)
    {
        String menuChoice = click.replace("menu-", "");
        data.setMenuChoice(menuChoice);
        data.setContent("<h1>Home</h1>\n");
    }

    private void handleSettings(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data)
    {
        String menuChoice = click.replace("menu-", "");
        data.setMenuChoice(menuChoice);
        data.setContent("<h1>Settings</h1>\n");
    }

    private void handleMenu(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        String menuChoice = click.replace("menu-", "");
        data.setMenuChoice(menuChoice);
        String subMenuChoice = "submenu-" + data.getSubMenuChoice(menuChoice);
        handleSubMenu(request, response, subMenuChoice, data, recordId);
    }

    private void handleSubMenu(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        String subMenu = click.replace("submenu-", "");
        data.putSubMenuChoice(data.getMenuChoice(), subMenu);
        System.err.println("SUBMENU choice: " + click + " with recordId: " + recordId);
        Menus.table(data, request, click);
    }

    private void handleRecordEdit(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD choice: " + click + " with recordId: " + recordId);
        Menus.edit(data, request, click, recordId);
    }

    private void handleRecordReEdit(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD choice: " + "record-reedit" + " with recordId: " + recordId);
        Menus.edit(data, request, "record-reedit", recordId);
    }

    private void handleRecordSave(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD SAVE: " + click + " with recordId: " + recordId);
        String cancelMethod = "clickMenu('submenu-" + data.getSubMenuChoice(data.getMenuChoice()) + "')";
        String reEditMethod = "clickRecordId('record-reedit', " + recordId + ")";
        try
        {
            data.fillPreviousParameterMap(request);
            if (data.getEditRecord().getTable().getName().toLowerCase().equals("user"))
                TableUser.saveUser(request, data, recordId);
            else
            {
                Table<?> table = (Table<?>) data.getEditRecord().getTable();
                UpdatableRecord<?> record =
                        recordId == 0 ? (UpdatableRecord<?>) data.getDSL().newRecord(table) : data.getEditRecord();
                String errors = ((TableForm) data.getEditForm()).setFields(record, request, data);
                if (errors.length() > 0)
                {
                    System.err.println(errors);
                    ModalWindowUtils.make2ButtonModalWindow(data, "Error storing record (1)", "<p>" + errors + "</p>", "Edit",
                            reEditMethod, "Cancel", cancelMethod, cancelMethod);
                }
                else
                {
                    try
                    {
                        record.store();
                    }
                    catch (Exception exception)
                    {
                        System.err.println(exception.getMessage());
                        System.err.println(record);
                        ModalWindowUtils.make2ButtonModalWindow(data, "Error storing record (2)",
                                "<p>" + exception.getMessage() + "</p>", "Edit", reEditMethod, "Cancel", cancelMethod,
                                cancelMethod);
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            ModalWindowUtils.make2ButtonModalWindow(data, "Error storing record (3)", "<p>" + e.getMessage() + "</p>", "Edit",
                    reEditMethod, "Cancel", cancelMethod, cancelMethod);
        }
        data.resetRoles();
        handleSubMenu(request, response, "submenu-" + data.getSubMenuChoice(data.getMenuChoice()), data, 0);
    }

    private void handleRecordCancel(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD CANCEL: " + click + " with recordId: " + recordId);
        if (data.getEditForm() instanceof TableForm)
        {
            TableForm tableForm = (TableForm) data.getEditForm();
            if (tableForm.checkFieldsChanged(data.getEditRecord(), request, data))
            {
                String cancelMethod = "clickMenu('submenu-" + data.getSubMenuChoice(data.getMenuChoice()) + "')";
                String reEditMethod = "clickRecordId('record-reedit', " + recordId + ")";
                data.fillPreviousParameterMap(request);
                ModalWindowUtils.make2ButtonModalWindow(data, "Data has changed",
                        "Data has changed. Do you want to continue editing or cancel without saving?", "Edit", reEditMethod,
                        "Cancel", cancelMethod, cancelMethod);
                handleRecordReEdit(request, response, click, data, recordId);
                return;
            }
        }

        handleSubMenu(request, response, "submenu-" + data.getSubMenuChoice(data.getMenuChoice()), data, 0);
    }

    private void handleRecordOk(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD OK: " + click + " with recordId: " + recordId);
        handleSubMenu(request, response, "submenu-" + data.getSubMenuChoice(data.getMenuChoice()), data, 0);
    }

    private void handleRecordDelete(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD DELETE: " + click + " with recordId: " + recordId);
        String cancelMethod = "clickMenu('submenu-" + data.getSubMenuChoice(data.getMenuChoice()) + "')";
        String deleteOkMethod = "clickRecordId('record-delete-ok', " + recordId + ")";
        ModalWindowUtils.make2ButtonModalWindow(data, "Delete confirmation", "Are you sure you want to delete this record?",
                "Delete", deleteOkMethod, "Cancel", cancelMethod, cancelMethod);
    }

    private void handleRecordDeleteOk(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("RECORD DELETE OK: " + click + " with recordId: " + recordId);
        try
        {
            var dslContext = data.getDSL();
            SubMenu tab = Menus.getActiveSubMenu(data);
            String tableName = tab.tableName();
            Table<?> table = DSL.table(DSL.name(tableName));
            Field<Integer> idField = DSL.field(DSL.name("id"), Integer.class);
            dslContext.delete(table).where(idField.eq(recordId)).execute();
            data.resetRoles();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            String okMethod = "clickMenu('submenu-" + data.getSubMenuChoice(data.getMenuChoice()) + "')";
            ModalWindowUtils.makeOkModalWindow("Error deleting record", "<p>" + exception.getMessage() + "</p>", okMethod);
        }
        handleSubMenu(request, response, "submenu-" + data.getSubMenuChoice(data.getMenuChoice()), data, 0);
    }

    // private void handleRecordSelect(final HttpServletRequest request, final HttpServletResponse response, final String click,
    // final AdminData data, final int recordId) throws IOException
    // {
    // System.err.println("RECORD SELECT: " + click + " with recordId: " + recordId);
    // var dslContext = data.getDSL();
    // SubMenu subMenu = Menus.getActiveSubMenu(data);
    // String table = subMenu.tableName();
    // Record tableRecord = dslContext.selectFrom(table).where("id=" + recordId).fetchAny();
    // String displayValue = tableRecord.get(subMenu.selectField()).toString();
    // data.setMenuFilterChoice(data.getSubMenuChoice(data.getMenuChoice()), recordId, displayValue);
    // handleTab(request, response, "submenu-" + data.getSubMenuChoice(data.getMenuChoice()), data, 0);
    // }

    private void handleCloseSelect(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("CLOSE SELECT: " + click);
        String tabName = click.substring(6);
        data.clearMenuFilterChoice(tabName);
        handleSubMenu(request, response, "submenu-" + data.getSubMenuChoice(data.getMenuChoice()), data, 0);
    }

    private void handleSort(final HttpServletRequest request, final HttpServletResponse response, final String click,
            final AdminData data, final int recordId) throws IOException
    {
        System.err.println("AZ choice: " + click + " with recordId: " + recordId);
        String fieldName = click.replace("az-", "");
        data.selectTableColumnSort(fieldName);
        Menus.table(data, request, "");
    }

}
