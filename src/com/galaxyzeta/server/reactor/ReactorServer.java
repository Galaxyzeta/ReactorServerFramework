package com.galaxyzeta.server.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * ReactorServer
 */
public abstract class ReactorServer {

	protected abstract void run();
}