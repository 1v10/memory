package dev.collinsthomas.memhogs.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import dev.collinsthomas.memhogs.BuildConfig
import dev.collinsthomas.memhogs.IShellService
import rikka.shizuku.Shizuku

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

private const val PERMISSION_REQUEST_CODE = 1
private const val TAG = "ShizukuConnection"
private const val SHELL_START_TIMEOUT_MILLIS = 10_000L
private const val SHELL_RECONNECT_MILLIS = 1_000L

class ShizukuConnection(private val listener: Listener) {

    interface Listener {
        fun onAccessChanged(access: ShizukuAccess)
        fun onShellConnected()
        fun onShellFailed(error: Throwable)
        fun onShellTimedOut()
    }

    var shell: IShellService? = null
        private set

    private var awaitingShell = false
    private var bound = false
    private var foreground = false
    private var started = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val reconnectShell = Runnable { if (foreground && started) evaluateAccess() }
    private val shellStartTimedOut = Runnable {
        awaitingShell = false
        listener.onShellTimedOut()
        scheduleReconnect()
    }

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            stopAwaitingShell()
            if (!started) return
            if (!binder.pingBinder()) {
                shell = null
                scheduleReconnect()
                return
            }
            mainHandler.removeCallbacks(reconnectShell)
            shell = IShellService.Stub.asInterface(binder)
            listener.onShellConnected()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            shell = null
            stopAwaitingShell()
            scheduleReconnect()
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { evaluateAccess() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        stopAwaitingShell()
        shell = null
        bound = false
        evaluateAccess()
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> evaluateAccess() }

    fun start() {
        started = true
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    fun stop() {
        started = false
        foreground = false
        mainHandler.removeCallbacks(reconnectShell)
        stopAwaitingShell()
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        unbindShell()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        if (value) {
            evaluateAccess()
        } else {
            mainHandler.removeCallbacks(reconnectShell)
        }
    }

    fun requestPermission() {
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    fun evaluateAccess() {
        if (!started) return
        val access = try {
            currentAccess()
        } catch (e: RuntimeException) {
            listener.onShellFailed(e)
            ShizukuAccess.NOT_RUNNING
        }
        listener.onAccessChanged(access)
        if (access != ShizukuAccess.READY || !foreground) return
        if (shell?.asBinder()?.pingBinder() == false) unbindShell()
        if (shell == null && !awaitingShell) bindShell()
    }

    fun reconnect() {
        stopAwaitingShell()
        unbindShell()
        if (foreground) scheduleReconnect()
    }

    private fun currentAccess(): ShizukuAccess = when {
        !Shizuku.pingBinder() -> ShizukuAccess.NOT_RUNNING
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuAccess.READY
        else -> ShizukuAccess.NEEDS_PERMISSION
    }

    private fun bindShell() {
        if (bound) unbindShell()
        awaitingShell = true
        mainHandler.postDelayed(shellStartTimedOut, SHELL_START_TIMEOUT_MILLIS)
        try {
            bound = true
            Shizuku.bindUserService(serviceArgs, serviceConnection)
        } catch (e: RuntimeException) {
            bound = false
            stopAwaitingShell()
            listener.onShellFailed(e)
            scheduleReconnect()
        }
    }

    private fun unbindShell() {
        shell = null
        if (!bound) return
        bound = false
        try {
            Shizuku.unbindUserService(serviceArgs, serviceConnection, true)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Shizuku was gone before the shell could be unbound", e)
        }
    }

    private fun scheduleReconnect() {
        mainHandler.removeCallbacks(reconnectShell)
        if (foreground && started) mainHandler.postDelayed(reconnectShell, SHELL_RECONNECT_MILLIS)
    }

    private fun stopAwaitingShell() {
        awaitingShell = false
        mainHandler.removeCallbacks(shellStartTimedOut)
    }
}
