package org.example;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class ShulkerViewerPlugin extends Plugin {
	public static final ShulkerViewer shulkerViewer = new ShulkerViewer();
	@Override
	public void onLoad() {	
		this.getLogger().info("Loading ShulkerViewer plugin...");
		RusherHackAPI.getModuleManager().registerFeature(shulkerViewer);
		this.getLogger().info("ShulkerViewer plugin loaded!");
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("ShulkerViewer plugin unloaded!");
	}
	
}
