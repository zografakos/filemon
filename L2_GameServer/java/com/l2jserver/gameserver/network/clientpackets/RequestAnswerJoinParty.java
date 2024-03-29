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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.PartyMatchRoom;
import com.l2jserver.gameserver.model.PartyMatchRoomList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExManagePartyRoomMember;
import com.l2jserver.gameserver.network.serverpackets.JoinParty;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 *  sample
 *  2a
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerJoinParty extends L2GameClientPacket
{
	private static final String _C__2A_REQUESTANSWERPARTY = "[C] 2A RequestAnswerJoinParty";
	//private static Logger _log = Logger.getLogger(RequestAnswerJoinParty.class.getName());
	
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		final L2PcInstance requestor = player.getActiveRequester();
		if (requestor == null)
			return;
		
		requestor.sendPacket(new JoinParty(_response));
		
		if (_response == 1)
		{
			if (requestor.isInParty())//Update by rocknow-Start
			{
				if (requestor.getParty().getMemberCount() >= 9)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.PARTY_FULL);
					player.sendPacket(sm);
					requestor.sendPacket(sm);
					return;
				}
			}//Update by rocknow-End
			player.joinParty(requestor.getParty());
			
			if(requestor.isInPartyMatchRoom() && player.isInPartyMatchRoom())
			{
				final PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if(list != null && (list.getPlayerRoomId(requestor) == list.getPlayerRoomId(player)))
				{
					final PartyMatchRoom room = list.getPlayerRoom(requestor);
					if (room != null)
					{
						final ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
						for(L2PcInstance member : room.getPartyMembers())
						{
							if (member != null)
								member.sendPacket(packet);
						}
					}
				}
			}
			else if (requestor.isInPartyMatchRoom() && !player.isInPartyMatchRoom())
			{
				final PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if(list != null)
				{
					final PartyMatchRoom room = list.getPlayerRoom(requestor);
					if (room != null)
					{
						room.addMember(player);
						ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
						for(L2PcInstance member : room.getPartyMembers())
						{
							if (member != null)
								member.sendPacket(packet);
						}
						player.setPartyRoom(room.getId());
						//player.setPartyMatching(1);
						player.broadcastUserInfo();
					}
				}
			}
		}
		else if (_response == -1)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_PARTY_REQUEST);
			sm.addPcName(player);
			requestor.sendPacket(sm);
			
			//activate garbage collection if there are no other members in party (happens when we were creating new one)
			if (requestor.isInParty() && requestor.getParty().getMemberCount() == 1)
				requestor.getParty().removePartyMember(requestor, false);
		}
		else // 0
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.PLAYER_DECLINED));
			
			//activate garbage collection if there are no other members in party (happens when we were creating new one)
			if (requestor.isInParty() && requestor.getParty().getMemberCount() == 1)
				requestor.getParty().removePartyMember(requestor, false);
		}
		
		
		if (requestor.isInParty())
			requestor.getParty().setPendingInvitation(false); // if party is null, there is no need of decreasing
		
		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__2A_REQUESTANSWERPARTY;
	}
}
