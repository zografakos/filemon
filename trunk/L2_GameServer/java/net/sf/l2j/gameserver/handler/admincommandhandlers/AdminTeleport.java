/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class handles following admin commands:
 * - show_moves
 * - show_teleport
 * - teleport_to_character
 * - move_to
 * - teleport_character
 *
 * @version $Revision: 1.3.2.6.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminTeleport implements IAdminCommandHandler
{
    private static final Logger _log = Logger.getLogger(AdminTeleport.class.getName());

    private static final String[] ADMIN_COMMANDS = {
        "admin_show_moves",
        "admin_show_moves_other",
        "admin_show_teleport",
        "admin_teleport_to_character",
        "admin_teleportto",
        "admin_move_to",
        "admin_teleport_character",
        "admin_recall",
        "admin_walk",
        "admin_explore",
        "teleportto",
        "recall",
        "admin_recall_npc",
        "admin_gonorth",
        "admin_gosouth",
        "admin_goeast",
        "admin_gowest",
        "admin_goup",
        "admin_godown",
        "admin_tele",
        "admin_teleto",
    };

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (command.equals("admin_teleto"))
        {
            activeChar.setTeleMode(1);
        }
        if (command.equals("admin_teleto r"))
        {
            activeChar.setTeleMode(2);
        }
        if (command.equals("admin_teleto end"))
        {
            activeChar.setTeleMode(0);
        }
        if (command.equals("admin_show_moves"))
        {
            AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
        }
        if (command.equals("admin_show_moves_other"))
        {
            AdminHelpPage.showHelpPage(activeChar, "tele/other.html");
        }
        else if (command.equals("admin_show_teleport"))
        {
            showTeleportCharWindow(activeChar);
        }
        else if (command.equals("admin_recall_npc"))
        {
            recallNPC(activeChar);
        }
        else if (command.equals("admin_teleport_to_character"))
        {
            teleportToCharacter(activeChar, activeChar.getTarget());
        }
        else if (command.equals("admin_explore") && Config.ACTIVATE_POSITION_RECORDER)
        {
            activeChar._exploring = ! activeChar._exploring;
            activeChar.explore();
        }
        else if (command.startsWith("admin_walk"))
        {
            try
            {
                String val = command.substring(11);
                StringTokenizer st = new StringTokenizer(val);
                String x1 = st.nextToken();
                int x = Integer.parseInt(x1);
                String y1 = st.nextToken();
                int y = Integer.parseInt(y1);
                String z1 = st.nextToken();
                int z = Integer.parseInt(z1);
                L2CharPosition pos = new L2CharPosition(x,y,z,0);
                activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,pos);
            }
            catch (Exception e)
            {
                if (Config.DEBUG) _log.info("admin_walk: "+e);
            }
        }
        else if (command.startsWith("admin_move_to"))
        {
            try
            {
                String val = command.substring(14);
                teleportTo(activeChar, val);
            }
            catch (StringIndexOutOfBoundsException e)
            {
                //Case of empty or missing coordinates
                AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
            }
        }
        else if (command.startsWith("admin_teleport_character"))
        {
            try
            {
                String val = command.substring(25);

           	    teleportCharacter(activeChar, val);
            }
            catch (StringIndexOutOfBoundsException e)
            {
                //Case of empty coordinates
                activeChar.sendMessage("Wrong or no Coordinates given.");
                showTeleportCharWindow(activeChar); //back to character teleport
            }
        }
        else if (command.startsWith("admin_teleportto "))
        {
            try
            {
                String targetName = command.substring(17);
                L2PcInstance player = L2World.getInstance().getPlayer(targetName);
                teleportToCharacter(activeChar, player);
            }
            catch (StringIndexOutOfBoundsException e)
            { }
        }
        else if (command.startsWith("admin_recall "))
        {
            try
            {
                String targetName = command.substring(13);
                L2PcInstance player = L2World.getInstance().getPlayer(targetName);

           	    teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
            }
            catch (StringIndexOutOfBoundsException e)
            { }
        }
        else if (command.equals("admin_tele"))
        {
            showTeleportWindow(activeChar);
        }
        else if (command.startsWith("admin_go"))
        {
        	int intVal=150;
        	int x = activeChar.getX(),y = activeChar.getY(),z = activeChar.getZ();
            try
            {
        	String val = command.substring(8);
        	StringTokenizer st = new StringTokenizer(val);
        	String dir=st.nextToken();
        	if (st.hasMoreTokens())
        		intVal = Integer.parseInt(st.nextToken());
        	if (dir.equals("east"))
        		x+=intVal;
        	else if (dir.equals("west"))
        		x-=intVal;
        	else if (dir.equals("north"))
        		y-=intVal;
        	else if (dir.equals("south"))
        		y+=intVal;
        	else if (dir.equals("up"))
        		z+=intVal;
        	else if (dir.equals("down"))
        		z-=intVal;
            activeChar.teleToLocation(x, y, z, false);
            showTeleportWindow(activeChar);
            }
            catch (Exception e)
            {
            	activeChar.sendMessage("Usage: //go<north|south|east|west|up|down> [offset] (default 150)");
            }
        }

        return true;
    }

    public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }

    private void teleportTo(L2PcInstance activeChar, String Cords)
    {
        try
        {
            StringTokenizer st = new StringTokenizer(Cords);
            String x1 = st.nextToken();
            int x = Integer.parseInt(x1);
            String y1 = st.nextToken();
            int y = Integer.parseInt(y1);
            String z1 = st.nextToken();
            int z = Integer.parseInt(z1);

            activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            activeChar.teleToLocation(x, y, z, false);

            activeChar.sendMessage("You have been teleported to " + Cords);
        }
        catch (NoSuchElementException nsee)
        {
            activeChar.sendMessage("Wrong or no Coordinates given.");
        }
    }


    private void showTeleportWindow(L2PcInstance activeChar)
    {
    	AdminHelpPage.showHelpPage(activeChar, "move.htm");
    }


    private void showTeleportCharWindow(L2PcInstance activeChar)
    {
        L2Object target = activeChar.getTarget();
        L2PcInstance player = null;
        if (target instanceof L2PcInstance)
        {
            player = (L2PcInstance)target;
        }
        else
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
            return;
        }
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

        TextBuilder replyMSG = new TextBuilder("<html><title>Teleport Character</title>");
        replyMSG.append("<body>");
        replyMSG.append("The character you will teleport is " + player.getName() + ".");
        replyMSG.append("<br>");

        replyMSG.append("Co-ordinate x");
        replyMSG.append("<edit var=\"char_cord_x\" width=110>");
        replyMSG.append("Co-ordinate y");
        replyMSG.append("<edit var=\"char_cord_y\" width=110>");
        replyMSG.append("Co-ordinate z");
        replyMSG.append("<edit var=\"char_cord_z\" width=110>");
        replyMSG.append("<button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
        replyMSG.append("<button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character " + activeChar.getX() + " " + activeChar.getY() + " " + activeChar.getZ() + "\" width=115 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
        replyMSG.append("<center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
        replyMSG.append("</body></html>");

        adminReply.setHtml(replyMSG.toString());
        activeChar.sendPacket(adminReply);
    }

    private void teleportCharacter(L2PcInstance activeChar , String Cords)
    {
        L2Object target = activeChar.getTarget();
        L2PcInstance player = null;
        if (target instanceof L2PcInstance)
        {
            player = (L2PcInstance)target;
        }
        else
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
            return;
        }

        if (player.getObjectId() == activeChar.getObjectId())
        {
        	player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
        }
        else
        {
            try
            {
                StringTokenizer st = new StringTokenizer(Cords);
                String x1 = st.nextToken();
                int x = Integer.parseInt(x1);
                String y1 = st.nextToken();
                int y = Integer.parseInt(y1);
                String z1 = st.nextToken();
                int z = Integer.parseInt(z1);
                teleportCharacter(player, x,y,z);
            } catch (NoSuchElementException nsee) {}
        }
    }

    /**
     * @param player
     * @param x
     * @param y
     * @param z
     */
    private void teleportCharacter(L2PcInstance player, int x, int y, int z)
    {
        if (player != null) {
            //Common character information
            player.sendMessage("Admin is teleporting you.");

            player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            player.teleToLocation(x, y, z, true);
        }
    }

    private void teleportToCharacter(L2PcInstance activeChar, L2Object target)
    {
        L2PcInstance player = null;
        if (target instanceof L2PcInstance)
        {
            player = (L2PcInstance)target;
        }
        else
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
            return;
        }

        if (player.getObjectId() == activeChar.getObjectId())
        {
        	player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
        }
        else
        {
            int x = player.getX();
            int y = player.getY();
            int z = player.getZ();

            activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            activeChar.teleToLocation(x, y, z, true);

            activeChar.sendMessage("You have teleported to character " + player.getName() + ".");
        }
    }
    private void recallNPC(L2PcInstance activeChar)
    {
        L2Object obj = activeChar.getTarget();
        if (obj instanceof L2NpcInstance)
        {
            L2NpcInstance target = (L2NpcInstance) obj;

            int monsterTemplate = target.getTemplate().npcId;
            L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(monsterTemplate);
            if (template1 == null)
            {
                activeChar.sendMessage("Incorrect monster template.");
                _log.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' template.");
                return;
            }

            L2Spawn spawn = target.getSpawn();
            if (spawn == null)
            {
                activeChar.sendMessage("Incorrect monster spawn.");
                _log.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' spawn.");
                return;
            }
            int respawnTime = spawn.getRespawnDelay();

            target.deleteMe();
            spawn.stopRespawn();
            SpawnTable.getInstance().deleteSpawn(spawn, true);

            try
            {
                //L2MonsterInstance mob = new L2MonsterInstance(monsterTemplate, template1);

                spawn = new L2Spawn(template1);
                spawn.setLocx(activeChar.getX());
                spawn.setLocy(activeChar.getY());
                spawn.setLocz(activeChar.getZ());
                spawn.setAmount(1);
                spawn.setHeading(activeChar.getHeading());
                spawn.setRespawnDelay(respawnTime);
                SpawnTable.getInstance().addNewSpawn(spawn, true);
                spawn.init();

                activeChar.sendMessage("Created " + template1.name + " on " + target.getObjectId() + ".");

                if (Config.DEBUG)
                {
                    _log.fine("Spawn at X="+spawn.getLocx()+" Y="+spawn.getLocy()+" Z="+spawn.getLocz());
                    _log.warning("GM: "+activeChar.getName()+"("+activeChar.getObjectId()+") moved NPC " + target.getObjectId());
                }
            }
            catch (Exception e)
            {
                activeChar.sendMessage("Target is not in game.");
            }

        }
        else
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
        }
    }

}
