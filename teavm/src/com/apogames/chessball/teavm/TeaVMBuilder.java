package com.apogames.chessball.teavm;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;

import java.io.File;

import org.teavm.vm.TeaVMOptimizationLevel;

public class TeaVMBuilder {
    public static void main(String[] args) {
        boolean startServer = args.length > 0 && args[0].equals("--serve");
        AssetFileHandle assetsPath = new AssetFileHandle("../assets");
        new TeaCompiler(new WebBackend().setHtmlTitle("ChessBall").setStartJettyAfterBuild(startServer))
                .addAssets(assetsPath)
                .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
                .setMainClass(TeaVMLauncher.class.getName())
                .setObfuscated(true)
                .build(new File("build/dist"));
    }
}
