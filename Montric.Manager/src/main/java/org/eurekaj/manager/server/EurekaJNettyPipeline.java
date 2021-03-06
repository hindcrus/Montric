package org.eurekaj.manager.server;

import java.io.File;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.eurekaj.api.util.IntegerParser;
import org.eurekaj.manager.server.handlers.*;
import org.eurekaj.manager.server.router.RouterHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

public class EurekaJNettyPipeline implements ChannelPipelineFactory {
	private static Logger logger = Logger.getLogger(EurekaJNettyPipeline.class.getName());
	private RouterHandler routerHandler = null;
	
	public EurekaJNettyPipeline() {
		
	}
	
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		String webappDir = System.getProperty("basedir");
		Integer cacheSeconds = IntegerParser.parseIntegerFromString(System.getProperty("org.eurekaj.indexCacheSeconds"), 0);
		
		ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("gzip", new HttpContentCompressor(6));
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

        if (routerHandler == null) {
        	logger.info("Building routerHandler for pipeline");
        	LinkedHashMap<String, ChannelHandler> routes = new LinkedHashMap<String, ChannelHandler>();
            routes.put("startsWith:/mainMenu.json", new MainMenuChannelHandler());
            routes.put("equals:/mainMenus", new MainMenuChannelHandler());
            routes.put("startsWith:/adminMenus", new MainMenuChannelHandler());
            routes.put("startsWith:/charts", new ChartChannelHandler());
            routes.put("startsWith:/alerts", new AlertChannelHandler());
            routes.put("startsWith:/chartGroups", new InstrumentationGroupChannelHandler());
            routes.put("startsWith:/accessToken", new AccessTokenHandler());
            routes.put("equals:/liveStatistics", new LiveStatisticsChannelHandler());
            routes.put("startsWith:/user", new UserChannelhandler());
            routes.put("startsWith:/account", new AccountHandler());
            routes.put("startsWith:/alertPlugins", new AlertPluginsHandler());
            routes.put("startsWith:/alertRecipients", new AlertRecipientsHandler());
            routes.put("startsWith:/cachedScript", new CachedScriptHandler(webappDir));
            routes.put("equals:/", new CachedIndexHandler(webappDir, cacheSeconds));
            routes.put("equals:/index.html", new CachedIndexHandler(webappDir, cacheSeconds));
             
    		routerHandler = new RouterHandler(routes, false, 
    			new CacheableFileServerHandler(
    				webappDir, 
    				IntegerParser.parseIntegerFromString(System.getProperty("org.eurekaj.indexCacheSeconds"), 0)
    			));
        }
        
        pipeline.addLast("handler_routeHandler", routerHandler);
        return pipeline;
	}

	
}
