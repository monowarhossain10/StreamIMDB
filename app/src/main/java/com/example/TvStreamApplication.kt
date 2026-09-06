package com.example

import android.app.Application
import android.system.Os

class TvStreamApplication : Application() {
    companion object {
        init {
            configureMesaEnvironment()
        }

        private fun configureMesaEnvironment() {
            try {
                // In cloud emulator / container environments without DRM rendernodes (/dev/dri/renderD128),
                // forcing software driver mode tells Mesa's loader to bypass hardware DRM device scanning,
                // preventing "Failed to open rendernode: No such file or directory" error logs.
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe", true)
                Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
            } catch (_: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        configureMesaEnvironment()
    }
}
