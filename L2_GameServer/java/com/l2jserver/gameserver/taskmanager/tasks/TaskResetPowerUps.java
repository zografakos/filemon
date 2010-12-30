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
package com.l2jserver.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.taskmanager.Task;
import com.l2jserver.gameserver.taskmanager.TaskManager;
import com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.l2jserver.gameserver.taskmanager.TaskTypes;


/**
 * Updates all data of Olympiad nobles in db
 * 
 * @author godson
 */
public class TaskResetPowerUps extends Task
{
	private static final Logger _log = Logger.getLogger(TaskResetPowerUps.class.getName());
	public static final String NAME = "reset_powerups";
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#onTimeElapsed(com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Calendar calendario = Calendar.getInstance();
		if ( calendario.DAY_OF_MONTH == 1 ) {
			resetOnlinePower();
			resetDBPower();
		}
		
	}
	public void resetOnlinePower() {
		for ( L2PcInstance player : L2World.getInstance().getAllPlayers().values()) {
			player.resetPowerUps();
		}
	}
	private void resetDBPower() {
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE player_powerups set trader=0,globalGatekeeper=0,aegisBlessing=0,mammonHunter=0,powerBuffer=0,noExp=0;");
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING,"Error while updating database values for Player Powerups");
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#initializate()
	 */
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "00:00:01", "");
	}
}
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
package com.l2jserver.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.taskmanager.Task;
import com.l2jserver.gameserver.taskmanager.TaskManager;
import com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.l2jserver.gameserver.taskmanager.TaskTypes;


/**
 * Updates all data of Olympiad nobles in db
 * 
 * @author godson
 */
public class TaskResetPowerUps extends Task
{
	private static final Logger _log = Logger.getLogger(TaskResetPowerUps.class.getName());
	public static final String NAME = "reset_powerups";
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#onTimeElapsed(com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Calendar calendario = Calendar.getInstance();
		if ( calendario.DAY_OF_MONTH == 1 ) {
			resetOnlinePower();
			resetDBPower();
		}
		
	}
	public void resetOnlinePower() {
		for ( L2PcInstance player : L2World.getInstance().getAllPlayers().values()) {
			player.resetPowerUps();
		}
	}
	private void resetDBPower() {
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE player_powerups set trader=0,globalGatekeeper=0,aegisBlessing=0,mammonHunter=0,powerBuffer=0,noExp=0;");
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING,"Error while updating database values for Player Powerups");
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#initializate()
	 */
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "00:00:01", "");
	}
}
