package io.github.dead_i.bungeeweb;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteStreams;

import io.github.dead_i.bungeeweb.api.ChangePassword;
import io.github.dead_i.bungeeweb.api.CreateUser;
import io.github.dead_i.bungeeweb.api.DeleteUser;
import io.github.dead_i.bungeeweb.api.EditUser;
import io.github.dead_i.bungeeweb.api.GetActivity;
import io.github.dead_i.bungeeweb.api.GetLang;
import io.github.dead_i.bungeeweb.api.GetLogs;
import io.github.dead_i.bungeeweb.api.GetServers;
import io.github.dead_i.bungeeweb.api.GetSession;
import io.github.dead_i.bungeeweb.api.GetStats;
import io.github.dead_i.bungeeweb.api.GetTypes;
import io.github.dead_i.bungeeweb.api.GetUUID;
import io.github.dead_i.bungeeweb.api.GetUsers;
import io.github.dead_i.bungeeweb.api.ListServers;
import io.github.dead_i.bungeeweb.hikari.HikariDB.UserProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.md_5.bungee.api.Favicon;

public class WebHandler extends AbstractHandler {
    
	private final @NotNull HashMap<String, APICommand> commands = new HashMap<>();
    private final @NotNull BungeeWeb plugin;
    private final @NotNull FaviconHandler faviconHandler = new FaviconHandler();

    public WebHandler(BungeeWeb plugin) {
        this.plugin = plugin;
        registerCommand(new ChangePassword(plugin));
        registerCommand(new CreateUser(plugin));
        registerCommand(new DeleteUser(plugin));
        registerCommand(new EditUser(plugin));
        registerCommand(new GetLang(plugin));
        registerCommand(new GetLogs(plugin));
        registerCommand(new GetServers(plugin));
        registerCommand(new GetSession(plugin));
        registerCommand(new GetStats(plugin));
        registerCommand(new GetTypes(plugin));
        registerCommand(new GetUsers(plugin));
        registerCommand(new GetUUID(plugin));
        registerCommand(new GetActivity(plugin));
        registerCommand(new ListServers(plugin));
    }

    @Override
    public void handle(String target, Request baseReq, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setCharacterEncoding("utf8");

        if (target.equals("/")) target = "/index.html";
        String[] path = target.split("/");

        if (target.substring(target.length() - 1).equals("/")) {
            res.sendRedirect(target.substring(0, target.length() - 1));
            baseReq.setHandled(true);
        } else if (target.equals("/favicon.ico")) {
        	res.setContentType("image/png png");
        	faviconHandler.writeFavicon(res.getOutputStream());
            baseReq.setHandled(true);
        } else if (path.length > 2 && path[1].equalsIgnoreCase("api")) {
            if (commands.containsKey(path[2])) {
                try {
                    APICommand command = commands.get(path[2]);
                    if (command.hasPermission(req)) {
                        command.execute(req, res, path);
                    }else{
                        res.getWriter().print("{ \"error\": \"You do not have permission to perform this action.\" }");
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("A MySQL database error occurred.");
                    e.printStackTrace();
                }
                baseReq.setHandled(true);
            }
        } else if (path.length > 1 && path[1].equalsIgnoreCase("login")) {
            Optional<UserProfile> profile = this.plugin.getDatabaseManager().getLogin(req.getParameter("user"), req.getParameter("pass"));
            if (req.getMethod().equals("POST") && profile.isPresent()) {
            	req.getSession().setAttribute("id", profile.get().id());
                req.getSession().setAttribute("user", profile.get().username());
                req.getSession().setAttribute("group", profile.get().groupId());
				res.getWriter().print("{ \"status\": 1 }");
            } else {
                res.getWriter().print("{ \"status\": 0 }");
            }
            baseReq.setHandled(true);
        }else if (path.length > 1 && path[1].equalsIgnoreCase("logout")) {
            req.getSession().invalidate();
            res.sendRedirect("/");
            baseReq.setHandled(true);
        }else if (target.equalsIgnoreCase("/css/theme.css")) {
            String name = this.plugin.getConfig().getString("server.theme");
            if (name.isEmpty()) name = "dark";
            InputStream resource = plugin.getResourceAsStream("themes/" + name + ".css");
            if (resource == null) {
                File file = new File(plugin.getDataFolder(), "themes/" + name + ".css");
                if (file.exists()) {
                	try (FileInputStream is = new FileInputStream(file)) {
                		ByteStreams.copy(is, res.getOutputStream());
                	}
                }
            }else{
                ByteStreams.copy(resource, res.getOutputStream());
            }
            baseReq.setHandled(true);
        } else {
            String file = "web" + target;
            InputStream stream = getFileOrResourceAsStream(file);
            if (stream == null && path.length == 2) {
                file = "web/index.html";
                stream = getFileOrResourceAsStream(file);
            }
            if (stream != null) {
                baseReq.setHandled(true);
                res.setContentType(getContentType(file));
                ByteStreams.copy(stream, res.getOutputStream());
            }
        }
    }

    public void registerCommand(APICommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public String getContentType(String filename) {
        MimetypesFileTypeMap map = new MimetypesFileTypeMap();
        map.addMimeTypes("text/html html htm");
        map.addMimeTypes("text/javascript js json");
        map.addMimeTypes("text/css css");
        map.addMimeTypes("image/jpeg jpg jpeg");
        map.addMimeTypes("image/gif gif");
        map.addMimeTypes("image/png png");
        return map.getContentType(filename.toLowerCase());
    }
    
    private InputStream getFileOrResourceAsStream(String stringPath) throws IOException {
    	stringPath = stringPath.replace("..", "\\.\\.");
    	Path path = this.plugin.getDataFolder().toPath().resolve(stringPath);
    	if (Files.isRegularFile(path)) return Files.newInputStream(path);
    	return this.plugin.getResourceAsStream(stringPath);
    }
    
    private class FaviconHandler {
    	
    	private long lastUpdate = 0;
    	private byte[] imageData = new byte[0];
    	
    	public void writeFavicon(OutputStream out) throws IOException {
    		if (System.currentTimeMillis() - this.lastUpdate > 300_000) { // 5 minutes
    			this.lastUpdate = System.currentTimeMillis();
    			@SuppressWarnings("deprecation")
				Favicon icon = plugin.getProxy().getConfig().getFaviconObject();
    			if (icon != null) {
    				BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(icon.getEncoded().split(",", 2)[1])));
    				BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
    				result.getGraphics().drawImage(image.getScaledInstance(result.getWidth(), result.getHeight(), Image.SCALE_DEFAULT), 0, 0, null);
    				ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    				ImageIO.write(result, "png", outBytes);
    				this.imageData = outBytes.toByteArray();
    			} else {
    				this.imageData = new byte[0];
    			}
    		}
			out.write(this.imageData);
    		
    	}
    	
    }
    
}
