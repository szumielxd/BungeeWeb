package me.szumielxd.proxyyweb.common.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteStreams;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetLang extends APICommand {
    
	public GetLang(@NotNull BungeeWeb plugin) {
        super(plugin, "getlang", true);
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String fallback = this.plugin.getConfig().getString("server.language");
        String lang = req.getParameter("lang");

        if (lang == null) lang = fallback;

        File file = new File(plugin.getDataFolder(), "lang/" + lang + ".json");
        InputStream stream;
        if (file.exists()) {
            stream = new FileInputStream(file);
        }else{
            stream = plugin.getResourceAsStream("lang/en.json");
        }

        ByteStreams.copy(stream, res.getOutputStream());
    }
}
