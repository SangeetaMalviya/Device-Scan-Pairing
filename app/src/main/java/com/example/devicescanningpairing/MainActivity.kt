package com.example.devicescanningpairing


import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var tvResults: TextView
    private lateinit var listViewDevices: ListView
    private val deviceList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private val port = 8080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        btnScan = findViewById(R.id.btnScan)
        tvResults = findViewById(R.id.tvResults)
        listViewDevices = findViewById(R.id.listViewDevices)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listViewDevices.adapter = adapter

        btnScan.setOnClickListener {
            scanDevicesOnNetwork("192.168.1", 1..254) { ip ->
                runOnUiThread {
                    deviceList.add(ip)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        listViewDevices.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = deviceList[position]
            connectToServer(selectedDevice, port)
        }

        // Start the server for pairing
        startServer(port)
    }

    private fun scanDevicesOnNetwork(
        subnet: String,
        range: IntRange,
        onDeviceFound: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            for (i in range) {
                val ip = "$subnet.$i"
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(100)) { // Ping the device
                        onDeviceFound(ip)
                    }
                } catch (e: Exception) {
                    // Handle unreachable IPs
                }
            }
        }
    }

    private fun startServer(port: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(port)
                println("Server started. Waiting for connections...")
                val socket = serverSocket.accept() // Wait for a client
                println("Client connected: ${socket.inetAddress.hostAddress}")

                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(socket.getOutputStream(), true)

                // Exchange messages
                output.println("Hello from Server!")
                println("Message from Client: ${input.readLine()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun connectToServer(serverIp: String, port: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(serverIp, port)
                println("Connected to server at $serverIp")

                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(socket.getOutputStream(), true)

                // Exchange messages
                println("Message from Server: ${input.readLine()}")
                output.println("Hello from Client!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
