package org.example;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * Example rusherhack plugin
 *
 * @author John200410
 */
public class ShulkerViewerPlugin extends Plugin {
	public static final ShulkerViewer shulkerViewer = new ShulkerViewer();
	@Override
	public void onLoad() {
		
		//logger
		this.getLogger().info("Hello World!");
		
		//creating and registering a new module
		RusherHackAPI.getModuleManager().registerFeature(shulkerViewer);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("Example plugin unloaded!");
	}
	
}