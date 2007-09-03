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

/**
 * Format: ch
 * c (id) 0xD0
 * h (subid) 0x08
 * @author -Wooden-
 *
 */
public final class RequestManorList extends L2GameClientPacket
{
	private static final String _C__FE_08_REQUESTMANORLIST = "[S] FE:08 RequestManorList";
	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		// just a trigger
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		//ExSendManorList manorlist = new ExSendManorList(/*get the manor list from somewhere*/);
		//sendPacket(manorlist);*/
		
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__FE_08_REQUESTMANORLIST;
	}
	
}