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
import com.l2jserver.gameserver.network.serverpackets.ExClosePartyRoom;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * 
 * @author Gnacik
 *
 */
public final class RequestWithdrawPartyRoom extends L2GameClientPacket
{
	private int _roomid;
	@SuppressWarnings("unused")
	private int _unk1;
	
	
	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_unk1 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance _activeChar = getClient().getActiveChar();
		
		if (_activeChar == null)
			return;
		
		PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_roomid);
		if (_room == null)
			return;
		
		if((_activeChar.isInParty() && _room.getOwner().isInParty()) && (_activeChar.getParty().getPartyLeaderOID() == _room.getOwner().getParty().getPartyLeaderOID()))
		{
			// If user is in party with Room Owner
			// is not removed from Room
			
			//_activeChar.setPartyMatching(0);
			_activeChar.broadcastUserInfo();
		}
		else
		{
			_room.deleteMember(_activeChar);
			
			_activeChar.setPartyRoom(0);
			//_activeChar.setPartyMatching(0);
			
			_activeChar.sendPacket(new ExClosePartyRoom());
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.PARTY_ROOM_EXITED));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:0B RequestWithdrawPartyRoom";
	}
	
}
