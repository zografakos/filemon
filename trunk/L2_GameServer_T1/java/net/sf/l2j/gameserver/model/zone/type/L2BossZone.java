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
package net.sf.l2j.gameserver.model.zone.type;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.util.L2FastList;

/**
 * @author DaRkRaGe
 */
public class L2BossZone extends L2ZoneType
{
    private String _zoneName;
    private int _timeInvade;
    
    // track the times that players got disconnected. Players are allowed
    // to log back into the zone as long as their log-out was within _timeInvade
    // time...
    // <player objectId, expiration time in milliseconds>
    private FastMap<Integer, Long> _playerAllowedReEntryTimes;

    // track the players admitted to the zone who should be allowed back in 
    // after reboot/server downtime (outside of their control), within 30 
    // of server restart
    private L2FastList<Integer> _playersAllowed;
    
    public L2BossZone(int id)
    {
        super(id);
        _playerAllowedReEntryTimes = new FastMap<Integer, Long>();
        _playersAllowed = new L2FastList<Integer>();
    }
    
    @Override
    public void setParameter(String name, String value)
    {
        if (name.equals("name"))
        {
            _zoneName = value;
        }
        
        if (name.equals("InvadeTime"))
        {
            _timeInvade = Integer.parseInt(value);
        }
        else
        {
            super.setParameter(name, value);
        }
    }
    
    @Override
    /**
     * Boss zones have special behaviors for player characters. Players are
     * automatically teleported out when the attempt to enter these zones,
     * except if the time at which they enter the zone is prior to the entry
     * expiration time set for that player. Entry expiration times are set by
     * any one of the following: 1) A player logs out while in a zone
     * (Expiration gets set to logoutTime + _timeInvade) 2) An external source
     * (such as a quest or AI of NPC) set up the player for entry.
     * 
     * There exists one more case in which the player will be allowed to enter.
     * That is if the server recently rebooted (boot-up time more recent than
     * currentTime - _timeInvade) AND the player was in the zone prior to reboot.
     */
    protected void onEnter(L2Character character)
    {
        if (character instanceof L2PcInstance)
        {
            //Thread.dumpStack();
            L2PcInstance player = (L2PcInstance) character;
            if (player.isGM())
            {
                player.sendMessage("You entered "
                        + _zoneName);
                return;
            }
            // if player has been (previously) cleared by npc/ai for entry and the zone is 
            // set to receive players (aka not waiting for boss to respawn)
            if (_playersAllowed.contains(character.getObjectId()))
            {
                // Get the information about this player's last logout-exit from
                // this zone.
                Long expirationTime = _playerAllowedReEntryTimes.get(character.getObjectId());
                
                // with legal entries, do nothing.
                if (expirationTime == null) // legal null expirationTime entries
                {
                    long serverStartTime = GameServer.dateTimeServerStarted.getTimeInMillis();
                    //long playerLastAccess = player.getLastAccess();
                    if ((serverStartTime > (System.currentTimeMillis() - _timeInvade)))
                            //&& (playerLastAccess < serverStartTime)
                            //&& (playerLastAccess > (serverStartTime - 4 * _timeInvade)))
                        return;
                }
                else 
                {
                    // legal non-null logoutTime entries
                    _playerAllowedReEntryTimes.remove(character.getObjectId());
                    if (expirationTime.longValue() > System.currentTimeMillis())
                        return;
                }
                _playersAllowed.remove(_playersAllowed.indexOf(character.getObjectId()));
            }
            // teleport out all players who attempt "illegal" (re-)entry
            player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
        }
    }
    
    @Override
    protected void onExit(L2Character character)
    {
        if (character instanceof L2PcInstance)
        {
            //Thread.dumpStack();
            L2PcInstance player = (L2PcInstance) character;
            if (player.isGM())
            {
                player.sendMessage("You left " + _zoneName);
                return;
            }
            // if the player just got disconnected/logged out, store the dc
            // time so that
            // decisions can be made later about allowing or not the player
            // to log into the zone
            if (player.isOnline() == 0 && _playersAllowed.contains(character.getObjectId()))
            {
                // mark the time that the player left the zone
                _playerAllowedReEntryTimes.put(character.getObjectId(), System.currentTimeMillis()
                        + _timeInvade);
            }
        }
    }
    
    public String getZoneName()
    {
        return _zoneName;
    }
    
    public int getTimeInvade()
    {
        return _timeInvade;
    }

    public void setAllowedPlayers(L2FastList<Integer> players)
    {
        if (players != null)
            _playersAllowed = players;
    }

    public L2FastList<Integer> getAllowedPlayers()
    {
        return _playersAllowed;
    }
    
    public boolean isPlayerAllowed(L2PcInstance player)
    {
        if (player.isGM())
            return true;
        else if (_playersAllowed.contains(player.getObjectId()))
            return true;
        else
        {
            player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            return false;
        }
     }
    
    /**
     * Occasionally, all players need to be sent out of the zone (for example,
     * if the players are just running around without fighting for too long, or
     * if all players die, etc). This call sends all online players to town and
     * marks offline players to be teleported (by clearing their relog
     * expiration times) when they log back in (no real need for off-line
     * teleport).
     */
    public void oustAllPlayers()
    {
        if (_characterList == null) return;
        if (_characterList.isEmpty()) return;
        for (L2Character character : _characterList.values())
        {
            if (character == null) continue;
            if (character instanceof L2PcInstance)
            {
                L2PcInstance player = (L2PcInstance) character;
                if (player.isOnline() == 1)
                    player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            }
        }
        _playerAllowedReEntryTimes.clear();
        _playersAllowed.clear();
    }
    
    /**
     * This function is to be used by external sources, such as quests and AI
     * in order to allow a player for entry into the zone for some time.  Naturally
     * if the player does not enter within the allowed time, he/she will be
     * teleported out again... 
     * @param player: reference to the player we wish to allow 
     * @param durationInSec: amount of time in seconds during which entry is valid.
     */
    public void allowPlayerEntry(L2PcInstance player, int durationInSec)
    {
        if (!player.isGM())
        {
            _playersAllowed.add(player.getObjectId());
            _playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + durationInSec*1000);

        }
    }
    
    @Override
    protected void onDieInside(L2Character character)
    {
    }
    
    @Override
    protected void onReviveInside(L2Character character)
    {
    }
}