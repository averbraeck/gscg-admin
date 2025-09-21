package org.gscg.admin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gscg.admin.form.WebForm;
import org.gscg.common.Access;
import org.gscg.common.CommonData;
import org.gscg.common.SqlUtils;
import org.gscg.data.Tables;
import org.gscg.data.tables.records.GameRecord;
import org.gscg.data.tables.records.GameSessionRecord;
import org.gscg.data.tables.records.OrganizationGameRecord;
import org.gscg.data.tables.records.OrganizationRecord;
import org.gscg.data.tables.records.UserRecord;
import org.jooq.Record;
import org.jooq.UpdatableRecord;

import jakarta.servlet.http.HttpServletRequest;

public class AdminData extends CommonData
{
    /** The name of the user logged in to this session. If null, no user is logged in. */
    private String username;

    /** the User record (static during session). */
    private UserRecord user;

    /** the access rights of the user via OrganizationRole. Lazy loading. */
    private Map<Integer, Access> organizationAccess = null;

    /** the access right of the user via GameRole. Lazy loading. */
    private Map<Integer, Access> gameAccess = null;

    /** the access right of the user via OrganizationGameRole. Lazy loading. */
    private Map<Integer, Access> organizationGameAccess = null;

    /** the access right of the user via GameSessionRole. Lazy loading. */
    private Map<Integer, Access> gameSessionAccess = null;

    /* ================================================ */
    /* PERSISTENT DATA ABOUT CHOICES MADE ON THE SCREEN */
    /* ================================================ */

    /** Which menu has been chosen, to maintain persistence after a POST. */
    private String menuChoice = "";

    /** Which submenu has been chosen, to maintain persistence after a POST. */
    private Map<String, String> subMenuChoice = new HashMap<>();

    /** Map that links the menu#tab name to a potential filter choice (record and display name) in the navbar. */
    private Map<String, FilterChoice> menuFilterChoices = new HashMap<>();

    /** The sorting order of columns in the tables. The map is from menu#tab to column header to A-Z / Z-A */
    private Map<String, ColumnSort> tableColumnSort = new HashMap<>();

    /** The topbar data. */
    private Topbar topbar = new Topbar();

    /** the page content as built by the appropriate class. */
    private String content = "";

    /** Show popup window or not. */
    private boolean showModalWindow = false;

    /** Modal window content for popup. */
    private String modalWindowHtml = "";

    /** The form that is currently being used. */
    private WebForm editForm = null;

    /** The record that is currently being edited. */
    private UpdatableRecord<?> editRecord = null;

    /** The parameters of the previous http-request to be able to re-edit a record (preserving the fields). */
    private Map<String, String> previousParameterMap;

    /** When the String is non-empty, an error occurred during save, delete, or cancel. */
    private String error = "";

    /** Record that has the field name and the direction of sorting; A-Z is true, Z-A is false. */
    public record ColumnSort(String fieldName, boolean az)
    {
    }

    /** A filter choice (record and display name), used in the filtering of records in the navbar. */
    public record FilterChoice(int recordId, String name)
    {
    }

    /* =================================== */
    /* GENERIC METHODS FOR THE DATA OBJECT */
    /* =================================== */

    public AdminData()
    {
        Menus.initializeSubMenuChoices(this);
    }

    public String makeSidebar()
    {
        return Sidebar.makeSidebar(this);
    }

    public String makeSubMenubar()
    {
        return SubMenubar.makeSubMenubar(this);
    }

    public String makeTopbar()
    {
        return this.topbar.makeTopbar(this);
    }

    public <R extends UpdatableRecord<R>> int getId(final R record)
    {
        return Provider.getId(record);
    }

    /* *********************** */
    /* ACCESS RIGHTS AND ROLES */
    /* *********************** */

    public boolean isPortalAdmin()
    {
        return getUser() == null ? false : getUser().getPortalAdmin() != 0;
    }

    public boolean isGameAdmin()
    {
        return hasGameAccess(Access.ADMIN);
    }

    public boolean isOrganizationAdmin()
    {
        return hasOrganizationAccess(Access.ADMIN);
    }

    public boolean hasOrganizationAccess(final Access access)
    {
        for (Access oa : getOrganizationAccess().values())
        {
            if (oa.ordinal() <= access.ordinal())
                return true;
        }
        return false;
    }

    public boolean hasOrganizationGameAccess(final Access access)
    {
        for (Access oga : getOrganizationGameAccess().values())
        {
            if (oga.ordinal() <= access.ordinal())
                return true;
        }
        return false;
    }

    public boolean hasGameAccess(final Access access)
    {
        for (Access ga : getGameAccess().values())
        {
            if (ga.ordinal() <= access.ordinal())
                return true;
        }
        return false;
    }

    public boolean hasGameSessionAccess(final Access access)
    {
        for (Access gsa : getGameSessionAccess().values())
        {
            if (gsa.ordinal() <= access.ordinal())
                return true;
        }
        return false;
    }

    /**
     * Call this method after adding or deleting users, organizations, games, organization-game combinations, game sessions,
     * dashboard templates, or after adding, changing or deleting roles.
     */
    public void resetRoles()
    {
        this.organizationAccess = null;
        this.gameAccess = null;
        this.organizationGameAccess = null;
        this.gameSessionAccess = null;
    }

    public Map<Integer, Access> getOrganizationAccess()
    {
        if (this.organizationAccess == null)
        {
            this.organizationAccess = new HashMap<>();
            if (isPortalAdmin())
            {
                List<OrganizationRecord> orgList = getDSL().selectFrom(Tables.ORGANIZATION).fetch();
                for (var organization : orgList)
                {
                    this.organizationAccess.put(organization.getId(), Access.ADMIN);
                }
            }
            /*-
            else
            {
                List<OrganizationRoleRecord> orList = getDSL().selectFrom(Tables.ORGANIZATION_ROLE)
                        .where(Tables.ORGANIZATION_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var or : orList)
                {
                    if (or.getAdmin() != 0)
                        this.organizationAccess.put(or.getOrganizationId(), Access.ADMIN);
                    else if (or.getEdit() != 0)
                        this.organizationAccess.put(or.getOrganizationId(), Access.EDIT);
                    else if (or.getView() != 0)
                        this.organizationAccess.put(or.getOrganizationId(), Access.VIEW);
                }
            }
            */
        }
        return this.organizationAccess;
    }

    public Set<Integer> getOrganizationAccess(final Access access)
    {
        Set<Integer> ret = new HashSet<>();
        for (var entry : getOrganizationAccess().entrySet())
        {
            if (entry.getValue().ordinal() <= access.ordinal())
                ret.add(entry.getKey());
        }
        return ret;
    }

    public Set<OrganizationRecord> getOrganizationPicklist(final Access access)
    {
        Set<OrganizationRecord> ret = new HashSet<>();
        for (var organizationEntry : getOrganizationAccess().entrySet())
        {
            if (organizationEntry.getValue().ordinal() <= access.ordinal())
            {
                var organization = SqlUtils.readRecordFromId(this, Tables.ORGANIZATION, organizationEntry.getKey());
                ret.add(organization);
            }
        }
        return ret;
    }

    public Map<Integer, Access> getGameAccess()
    {
        if (this.gameAccess == null)
        {
            this.gameAccess = new HashMap<>();

            if (isPortalAdmin())
            {
                List<GameRecord> gameList = getDSL().selectFrom(Tables.GAME).fetch();
                for (var game : gameList)
                {
                    this.gameAccess.put(game.getId(), Access.EDIT);
                }
            }
            /*-
            else
            
            {
                // direct game roles
                List<GameRoleRecord> grList =
                        getDSL().selectFrom(Tables.GAME_ROLE).where(Tables.GAME_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var gr : grList)
                {
                    if (gr.getEdit() != 0)
                        addGameAccess(gr.getGameId(), Access.EDIT);
                    else if (gr.getView() != 0)
                        addGameAccess(gr.getGameId(), Access.VIEW);
                }
            
                // indirect game roles via game_access for all organizations where user is a member
                List<OrganizationRoleRecord> orList = getDSL().selectFrom(Tables.ORGANIZATION_ROLE)
                        .where(Tables.ORGANIZATION_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var or : orList)
                {
                    List<OrganizationGameRecord> ogList = getDSL().selectFrom(Tables.ORGANIZATION_GAME)
                            .where(Tables.ORGANIZATION_GAME.ORGANIZATION_ID.eq(or.getOrganizationId())).fetch();
                    for (var og : ogList)
                    {
                        if (or.getEdit() != 0 || or.getAdmin() != 0 || or.getView() != 0)
                            addGameAccess(og.getGameId(), Access.VIEW);
                    }
                }
            
                // indirect game roles via direct game_access role
                List<OrganizationGameRoleRecord> ogrList = getDSL().selectFrom(Tables.ORGANIZATION_GAME_ROLE)
                        .where(Tables.ORGANIZATION_GAME_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var ogr : ogrList)
                {
                    OrganizationGameRecord ga =
                            SqlUtils.readRecordFromId(this, Tables.ORGANIZATION_GAME, ogr.getOrganizationGameId());
                    if (ogr.getEdit() != 0 || ogr.getView() != 0)
                        addGameAccess(ga.getGameId(), Access.VIEW);
                }
            
                // indirect game roles via session_role
                List<GameSessionRoleRecord> gsrList = getDSL().selectFrom(Tables.GAME_SESSION_ROLE)
                        .where(Tables.GAME_SESSION_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var gsr : gsrList)
                {
                    GameSessionRecord gs = SqlUtils.readRecordFromId(this, Tables.GAME_SESSION, gsr.getGameSessionId());
                    GameVersionRecord gv = SqlUtils.readRecordFromId(this, Tables.GAME_VERSION, gs.getGameVersionId());
                    if (gsr.getEdit() != 0 || gsr.getView() != 0)
                        addGameAccess(gv.getGameId(), Access.VIEW);
                }
            
                // indirect game roles via dashboard_role
                List<DashboardRoleRecord> drList = getDSL().selectFrom(Tables.DASHBOARD_ROLE)
                        .where(Tables.DASHBOARD_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var dr : drList)
                {
                    DashboardTemplateRecord dt =
                            SqlUtils.readRecordFromId(this, Tables.DASHBOARD_TEMPLATE, dr.getDashboardTemplateId());
                    GameVersionRecord gv = SqlUtils.readRecordFromId(this, Tables.GAME_VERSION, dt.getGameVersionId());
                    if (dr.getEdit() != 0 || dr.getView() != 0)
                        addGameAccess(gv.getGameId(), Access.VIEW);
                }
            }
            */
        }
        return this.gameAccess;
    }

    private void addGameAccess(final Integer gameId, final Access access)
    {
        Access oldAccess = this.gameAccess.get(gameId);
        if (oldAccess == null)
            this.gameAccess.put(gameId, access);
        else if (oldAccess.ordinal() > access.ordinal())
            this.gameAccess.put(gameId, access);
    }

    public Set<Integer> getGameAccess(final Access access)
    {
        Set<Integer> ret = new HashSet<>();
        for (var entry : getGameAccess().entrySet())
        {
            if (entry.getValue().ordinal() <= access.ordinal())
                ret.add(entry.getKey());
        }
        return ret;
    }

    public Set<GameRecord> getGamePicklist(final Access access)
    {
        Set<GameRecord> ret = new HashSet<>();
        for (var gameEntry : getGameAccess().entrySet())
        {
            if (gameEntry.getValue().ordinal() <= access.ordinal())
            {
                var game = SqlUtils.readRecordFromId(this, Tables.GAME, gameEntry.getKey());
                ret.add(game);
            }
        }
        return ret;
    }

    public Set<GameRecord> getGamePicklist(final int organizationId, final Access access)
    {
        Set<GameRecord> ret = new HashSet<>();
        for (var gameEntry : getGameAccess().entrySet())
        {
            if (gameEntry.getValue().ordinal() <= access.ordinal())
            {
                for (var orgGameId : getOrganizationGameAccess(Access.EDIT))
                {
                    OrganizationGameRecord og = SqlUtils.readRecordFromId(this, Tables.ORGANIZATION_GAME, orgGameId);
                    if (og.getOrganizationId().equals(organizationId) && gameEntry.getKey().equals(og.getGameId()))
                    {
                        var game = SqlUtils.readRecordFromId(this, Tables.GAME, gameEntry.getKey());
                        ret.add(game);
                    }
                }
            }
        }
        return ret;
    }

    public Map<Integer, String> getGameVersionPicklist(final Access access)
    {
        Map<Integer, String> ret = new HashMap<>();
        List<Record> gvList =
                getDSL().selectFrom(Tables.GAME_VERSION.join(Tables.GAME).on(Tables.GAME_VERSION.GAME_ID.eq(Tables.GAME.ID)))
                    .fetch();
        for (var gv : gvList)
        {
            for (var gameEntry : getGameAccess().entrySet())
            {
                if (gameEntry.getValue().ordinal() <= access.ordinal()
                        && gameEntry.getKey().equals(gv.getValue(Tables.GAME.ID)))
                {
                    ret.put(gv.getValue(Tables.GAME_VERSION.ID),
                            gv.getValue(Tables.GAME.CODE) + "-" + gv.getValue(Tables.GAME_VERSION.NAME));
                }
            }
        }
        return ret;
    }

    public Map<Integer, String> getGameVersionPicklist(final int gameId, final Access access)
    {
        Map<Integer, String> ret = new HashMap<>();
        List<Record> gvList =
                getDSL().selectFrom(Tables.GAME_VERSION.join(Tables.GAME).on(Tables.GAME_VERSION.GAME_ID.eq(Tables.GAME.ID)))
                    .where(Tables.GAME.ID.eq(gameId))
                    .fetch();
        for (var gv : gvList)
        {
            for (var gameEntry : getGameAccess().entrySet())
            {
                if (gameEntry.getValue().ordinal() <= access.ordinal()
                        && gameEntry.getKey().equals(gv.getValue(Tables.GAME.ID)))
                {
                    ret.put(gv.getValue(Tables.GAME_VERSION.ID),
                            gv.getValue(Tables.GAME.CODE) + "-" + gv.getValue(Tables.GAME_VERSION.NAME));
                }
            }
        }
        return ret;
    }

    public Map<Integer, Access> getOrganizationGameAccess()
    {
        if (this.organizationGameAccess == null)
        {
            this.organizationGameAccess = new HashMap<>();

            if (isPortalAdmin())
            {
                List<OrganizationGameRecord> ogList = getDSL().selectFrom(Tables.ORGANIZATION_GAME).fetch();
                for (var og : ogList)
                {
                    this.organizationGameAccess.put(og.getId(), Access.EDIT);
                }
            }
            /*-
            else
            
            {
                // direct game access roles
                List<OrganizationGameRoleRecord> ogrList = getDSL().selectFrom(Tables.ORGANIZATION_GAME_ROLE)
                        .where(Tables.ORGANIZATION_GAME_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var ogr : ogrList)
                {
                    if (ogr.getEdit() != 0)
                        addOrganizationGameAccess(ogr.getOrganizationGameId(), Access.EDIT);
                    else if (ogr.getView() != 0)
                        addOrganizationGameAccess(ogr.getOrganizationGameId(), Access.VIEW);
                }
            
                // indirect game_access roles for all organizations where user is a member
                List<OrganizationRoleRecord> orList = getDSL().selectFrom(Tables.ORGANIZATION_ROLE)
                        .where(Tables.ORGANIZATION_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var or : orList)
                {
                    List<OrganizationGameRecord> ogList = getDSL().selectFrom(Tables.ORGANIZATION_GAME)
                            .where(Tables.ORGANIZATION_GAME.ORGANIZATION_ID.eq(or.getOrganizationId())).fetch();
                    for (var og : ogList)
                    {
                        if (or.getEdit() != 0 || or.getAdmin() != 0)
                            addOrganizationGameAccess(og.getId(), Access.EDIT);
                        if (or.getView() != 0)
                            addOrganizationGameAccess(og.getId(), Access.VIEW);
                    }
                }
            }
            */
        }
        return this.organizationGameAccess;
    }

    public Set<Integer> getOrganizationGameAccess(final Access access)
    {
        Set<Integer> ret = new HashSet<>();
        for (var entry : getOrganizationGameAccess().entrySet())
        {
            if (entry.getValue().ordinal() <= access.ordinal())
                ret.add(entry.getKey());
        }
        return ret;
    }

    public Set<OrganizationGameRecord> getOrganizationGamePicklist(final Access access)
    {
        Set<OrganizationGameRecord> ret = new HashSet<>();
        for (var entry : getOrganizationGameAccess().entrySet())
        {
            if (entry.getValue().ordinal() <= access.ordinal())
            {
                var og = SqlUtils.readRecordFromId(this, Tables.ORGANIZATION_GAME, entry.getKey());
                ret.add(og);
            }
        }
        return ret;
    }

    private void addOrganizationGameAccess(final Integer organizationGameId, final Access access)
    {
        Access oldAccess = this.organizationGameAccess.get(organizationGameId);
        if (oldAccess == null)
            this.organizationGameAccess.put(organizationGameId, access);
        else if (oldAccess.ordinal() > access.ordinal())
            this.organizationGameAccess.put(organizationGameId, access);
    }

    public Map<Integer, Access> getGameSessionAccess()
    {
        if (this.gameSessionAccess == null)
        {
            this.gameSessionAccess = new HashMap<>();

            if (isPortalAdmin())
            {
                List<GameSessionRecord> gsList = getDSL().selectFrom(Tables.GAME_SESSION).fetch();
                for (var gs : gsList)
                {
                    this.gameSessionAccess.put(gs.getId(), Access.EDIT);
                }
            }
            /*-
            else
            
            {
                // direct game session roles
                List<GameSessionRoleRecord> gsrList = getDSL().selectFrom(Tables.GAME_SESSION_ROLE)
                        .where(Tables.GAME_SESSION_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var gsr : gsrList)
                {
                    if (gsr.getEdit() != 0)
                        addGameGameSessionAccess(gsr.getGameSessionId(), Access.EDIT);
                    else if (gsr.getView() != 0)
                        addGameGameSessionAccess(gsr.getGameSessionId(), Access.VIEW);
                }
            
                // indirect game session roles via organization_game roles
                List<Record> ogrList = getDSL()
                        .selectFrom(Tables.ORGANIZATION_GAME.join(Tables.ORGANIZATION_GAME_ROLE)
                                .on(Tables.ORGANIZATION_GAME_ROLE.ORGANIZATION_GAME_ID.eq(Tables.ORGANIZATION_GAME.ID)))
                        .where(Tables.ORGANIZATION_GAME_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var ogr : ogrList)
                {
                    List<Record> gsList = getDSL()
                            .selectFrom(Tables.GAME_SESSION.join(Tables.GAME_VERSION)
                                    .on(Tables.GAME_SESSION.GAME_VERSION_ID.eq(Tables.GAME_VERSION.ID)))
                            .where(Tables.GAME_SESSION.ORGANIZATION_ID
                                    .eq(ogr.getValue(Tables.ORGANIZATION_GAME.ORGANIZATION_ID))
                                    .and(Tables.GAME_VERSION.GAME_ID.eq(ogr.getValue(Tables.ORGANIZATION_GAME.GAME_ID))))
                            .fetch();
                    for (var gs : gsList)
                    {
                        if (ogr.getValue(Tables.ORGANIZATION_GAME_ROLE.EDIT) != 0)
                            addGameGameSessionAccess(gs.getValue(Tables.GAME_SESSION.ID), Access.EDIT);
                        else if (ogr.getValue(Tables.ORGANIZATION_GAME_ROLE.VIEW) != 0)
                            addGameGameSessionAccess(gs.getValue(Tables.GAME_SESSION.ID), Access.VIEW);
                    }
                }
            
                // indirect game session roles for all organizations where user is a member
                List<OrganizationRoleRecord> orList = getDSL().selectFrom(Tables.ORGANIZATION_ROLE)
                        .where(Tables.ORGANIZATION_ROLE.USER_ID.eq(this.user.getId())).fetch();
                for (var or : orList)
                {
                    List<OrganizationGameRecord> ogList = getDSL().selectFrom(Tables.ORGANIZATION_GAME)
                            .where(Tables.ORGANIZATION_GAME.ORGANIZATION_ID.eq(or.getOrganizationId())).fetch();
                    for (var og : ogList)
                    {
                        List<GameSessionRecord> gsList = getDSL().selectFrom(Tables.GAME_SESSION)
                                .where(Tables.GAME_SESSION.ORGANIZATION_ID.eq(og.getOrganizationId())).fetch();
                        for (var gs : gsList)
                        {
                            if (or.getEdit() != 0)
                                addGameGameSessionAccess(gs.getId(), Access.EDIT);
                            else if (or.getView() != 0)
                                addGameGameSessionAccess(gs.getId(), Access.VIEW);
                        }
                    }
                }
            }
            */
        }
        return this.gameSessionAccess;
    }

    private void addGameGameSessionAccess(final Integer gameSessionId, final Access access)
    {
        Access oldAccess = this.gameSessionAccess.get(gameSessionId);
        if (oldAccess == null)
            this.gameSessionAccess.put(gameSessionId, access);
        else if (oldAccess.ordinal() > access.ordinal())
            this.gameSessionAccess.put(gameSessionId, access);
    }

    public Set<Integer> getGameSessionAccess(final Access access)
    {
        Set<Integer> ret = new HashSet<>();
        for (var entry : getGameSessionAccess().entrySet())
        {
            if (entry.getValue().ordinal() <= access.ordinal())
                ret.add(entry.getKey());
        }
        return ret;
    }

    public Set<GameSessionRecord> getGameSessionPicklist(final Access access)
    {
        Set<GameSessionRecord> ret = new HashSet<>();
        for (var entry : getGameSessionAccess().entrySet())
        {
            if (entry.getValue().ordinal() <= access.ordinal())
            {
                var gameSession = SqlUtils.readRecordFromId(this, Tables.GAME_SESSION, entry.getKey());
                ret.add(gameSession);
            }
        }
        return ret;
    }

    /* ************************ */
    /* DATABASE AND FORM ACCESS */
    /* ************************ */

    public void setEditForm(final WebForm editForm)
    {
        System.err.println("setEditForm is called!!!");
        this.editForm = editForm;
    }

    public void setEditRecord(final UpdatableRecord<?> editRecord)
    {
        this.editRecord = editRecord;
    }

    /* ******************* */
    /* GETTERS AND SETTERS */
    /* ******************* */

    public String getUsername()
    {
        return this.username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public UserRecord getUser()
    {
        return this.user;
    }

    public void setUser(final UserRecord user)
    {
        this.user = user;
    }

    public String getMenuChoice()
    {
        return this.menuChoice;
    }

    public void setMenuChoice(final String menuChoice)
    {
        this.menuChoice = menuChoice;
    }

    public String getSubMenuChoice(final String menuChoice)
    {
        return this.subMenuChoice.get(menuChoice);
    }

    public void putSubMenuChoice(final String menuChoice, final String subMenuChoice)
    {
        this.subMenuChoice.put(menuChoice, subMenuChoice);
    }

    public Topbar getTopbar()
    {
        return this.topbar;
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent(final String content)
    {
        this.content = content;
    }

    public boolean isShowModalWindow()
    {
        return this.showModalWindow;
    }

    public void setShowModalWindow(final boolean showModalWindow)
    {
        this.showModalWindow = showModalWindow;
    }

    public String getModalWindowHtml()
    {
        return this.modalWindowHtml;
    }

    public void setModalWindowHtml(final String modalWindowHtml)
    {
        this.modalWindowHtml = modalWindowHtml;
    }

    public ColumnSort getTableColumnSort()
    {
        return this.tableColumnSort.get(this.menuChoice + "#" + getSubMenuChoice(this.menuChoice));
    }

    public void selectTableColumnSort(final String fieldName)
    {
        String fn = fieldName.toLowerCase().replace(' ', '-');
        String key = this.menuChoice + "#" + getSubMenuChoice(this.menuChoice);
        ColumnSort oldColumnSort = getTableColumnSort();
        if (oldColumnSort != null && fn.equals(oldColumnSort.fieldName()))
            this.tableColumnSort.put(key, new ColumnSort(fn, !oldColumnSort.az()));
        else
            this.tableColumnSort.put(key, new ColumnSort(fn, true));
    }

    public FilterChoice getMenuFilterChoice(final String filterChoice)
    {
        return this.menuFilterChoices.get(this.menuChoice + "#" + filterChoice);
    }

    public void setMenuFilterChoice(final String filterChoice, final int recordId, final String displayName)
    {
        this.menuFilterChoices.put(this.menuChoice + "#" + filterChoice, new FilterChoice(recordId, displayName));
    }

    public void clearMenuFilterChoice(final String filterChoice)
    {
        this.menuFilterChoices.remove(this.menuChoice + "#" + filterChoice);
    }

    public String getError()
    {
        return this.error;
    }

    public void setError(final String error)
    {
        this.error = error;
    }

    public WebForm getEditForm()
    {
        return this.editForm;
    }

    public UpdatableRecord<?> getEditRecord()
    {
        return this.editRecord;
    }

    public Map<String, String> getPreviousParameterMap()
    {
        return this.previousParameterMap;
    }

    public void fillPreviousParameterMap(final HttpServletRequest previousRequest)
    {
        this.previousParameterMap = new HashMap<>();
        for (var entry : previousRequest.getParameterMap().entrySet())
            this.previousParameterMap.put(entry.getKey(), entry.getValue()[0]);
    }

}
