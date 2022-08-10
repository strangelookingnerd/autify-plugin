package io.jenkins.plugins.autify;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

public class AutifyCli {

    protected final FilePath workspace;
    protected final Launcher launcher;
    protected final PrintStream logger;
    protected String autifyPath = "./autify/bin/autify";
    private String webAccessToken = "";
    private String mobileAccessToken = "";

    public AutifyCli(FilePath workspace, Launcher launcher, TaskListener listener) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.logger = listener.getLogger();
    }

    public int install() {
        return runShellScript("AutifyCli/install.sh");
    }

    public int webTestRun(String autifyUrl, boolean wait) {
        ArgumentListBuilder builder = autifyBuilder("web", "test", "run");
        builder.add(autifyUrl);
        if (wait) builder.add("--wait");
        return runCommand(builder);
    }

    public void webAuthLogin(String webAccessToken) {
        this.webAccessToken = webAccessToken;
    }

    public void mobileAuthLogin(String mobileAccessToken) {
        this.mobileAccessToken = mobileAccessToken;
    }

    private ArgumentListBuilder autifyBuilder(String... arguments) {
        ArgumentListBuilder builder = new ArgumentListBuilder(autifyPath);
        builder.add(arguments);
        return builder;
    }

    protected int runCommand(ArgumentListBuilder builder) {
        return runCommand(builder.toCommandArray());
    }

    protected int runCommand(String... command) {
        try {
            return launcher.launch()
                .pwd(workspace)
                .envs(getEnvs())
                .stdout(logger)
                .stderr(logger)
                .cmds(command)
                .start()
                .join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(logger);
            return 1;
        }
    }

    protected Map<String, String> getEnvs() {
        Map<String, String> envs = new HashMap<String, String>();
        envs.put("AUTIFY_WEB_ACCESS_TOKEN", webAccessToken);
        envs.put("AUTIFY_MOBILE_ACCESS_TOKEN", mobileAccessToken);
        envs.put("XDG_DATA_HOME", workspace + "/.config");
        return envs;
    }

    protected String getResourcePath(String name) {
        URL url = getClass().getResource(name);
        if (url == null) return null;
        return url.getPath();
    }

    protected int runShellScript(String scriptName) {
        String scriptPath = getResourcePath(scriptName);
        if (scriptPath == null) {
            logger.println("Cannot find the script '" + scriptName + "'");
            return 1;
        }
        return runCommand("bash", "-xe", scriptPath);
    }

    public static class Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCli(workspace, launcher, listener);
        }
    }
}
