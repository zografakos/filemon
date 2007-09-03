/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.clientpackets;

import java.sql.PreparedStatement;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  5F 
 *  01 00 00 00
 * 
 *  format  cdd
 * 
 * 
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerFriendInvite extends L2GameClientPacket
{
	private static final String _C__5F_REQUESTANSWERFRIENDINVITE = "[C] 5F RequestAnswerFriendInvite";
	private static Logger _log = Logger.getLogger(RequestAnswerFriendInvite.class.getName());
	
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
    	if(player != null)
        {
    		L2PcInstance requestor = player.getActiveRequester();
    		if (requestor == null)
    		    return;

    		if (_response == 1) 
            {
        		java.sql.Connection con = null;
        		try 
        		{
        		    con = L2DatabaseFactory.getInstance().getConnection();
        		    PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (char_id, friend_id, friend_name) VALUES (?, ?, ?), (?, ?, ?)");
                    statement.setInt(1, requestor.getObjectId());
                    statement.setInt(2, player.getObjectId());
        		    statement.setString(3, player.getName());
                    statement.setInt(4, player.getObjectId());
                    statement.setInt(5, requestor.getObjectId());
                    statement.setString(6, requestor.getName());
        		    statement.execute();
        		    statement.close();
        			SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);
        			requestor.sendPacket(msg);
                    
        			//Player added to your friendlist
            		msg = new SystemMessage(SystemMessageId.S1_ADDED_TO_FRIENDS);
        			msg.addString(player.getName());
            		requestor.sendPacket(msg);
                    
        			//has joined as friend.
            		msg = new SystemMessage(SystemMessageId.S1_JOINED_AS_FRIEND);
        			msg.addString(requestor.getName());
            		player.sendPacket(msg);
            		msg = null;
        		} 
        		catch (Exception e)
        		{
        		    _log.warning("could not add friend objectid: "+ e);
        		}
        		finally
        		{
        		    try { con.close(); } catch (Exception e) {}
        		}
    		} else 
            {
    			SystemMessage msg = new SystemMessage(SystemMessageId.FAILED_TO_INVITE_A_FRIEND);
    			requestor.sendPacket(msg);
    			msg = null;
    		}
    		
    		player.setActiveRequester(null);
    		requestor.onTransactionResponse();
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__5F_REQUESTANSWERFRIENDINVITE;
	}
}
