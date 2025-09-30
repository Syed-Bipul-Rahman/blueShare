# BlueShare User Guide

## How to Share Files Between Two Devices

### Step-by-Step Instructions

#### Setup (Do this on BOTH devices)

1. **Install the App**
   - Install BlueShare APK on both devices
   - Open the app on both devices

2. **Grant Permissions**
   When the app opens for the first time, grant these permissions:
   - ✅ **Location** - Required for Wi-Fi Direct and Bluetooth discovery
   - ✅ **Nearby Devices** - Required for device scanning
   - ✅ **Bluetooth** - Required for Bluetooth transfers
   - ✅ **Photos and Videos** - Required to access files
   - ✅ **Music and Audio** - Required to access audio files
   - ✅ **Notifications** - Required for transfer progress updates

   **Important:** Both devices must grant all permissions for the app to work properly.

---

#### Sending Files

**On Device A (Sender):**

1. Tap the **"Send Files"** button on the home screen

2. You'll see a screen that says "Select files and choose a device"

3. Tap **"Select Files"** button
   - A file picker will open
   - Select one or more files you want to send
   - Files can be photos, videos, documents, etc.

4. Wait for device discovery
   - The app will show "Discovering devices..."
   - Available devices will appear in a list
   - You should see "Device B" in the list

5. Tap on **Device B** from the list
   - A dialog will ask "Connect to Device?"
   - Tap **"Connect"**

6. Wait for connection
   - The app will show "Connecting to Device B..."
   - When connected, you'll see "Connected to Device B"
   - A **"Start Transfer"** button will appear

7. Tap **"Start Transfer"**
   - Files will begin transferring
   - You'll see:
     - Progress bar (0-100%)
     - Transfer speed (e.g., "5.2 MB/s")
     - Time remaining
     - Current file being transferred

8. Transfer complete!
   - A success dialog will appear
   - Shows number of files transferred, size, and time taken

**On Device B (Receiver):**

1. Tap the **"Receive Files"** button on the home screen

2. You'll see a screen that says "Waiting for sender to connect..."

3. **Keep this screen open** while Device A discovers and connects

4. When Device A connects:
   - The app will show "Connected to Device A"
   - Files will automatically start receiving

5. Monitor the transfer
   - You'll see the same progress information as Device A

6. Transfer complete!
   - Files will be saved to your Downloads folder
   - A success message will appear

---

### Current Functionality (v0.2)

✅ **Working Features:**
- Device discovery (Wi-Fi Direct and Bluetooth)
- Device list with real-time updates
- File picker for selecting multiple files
- Connection management
- Transfer progress UI
- Send/Receive mode switching
- Permission handling
- Material Design 3 UI

⚠️ **In Development:**
- Actual file transfer over sockets (currently simulated)
- Pause/Resume functionality
- Transfer history
- Background transfers via Foreground Service

---

### Troubleshooting

#### Problem: "No devices found"

**Solutions:**
1. Make sure **both devices** have the app open
2. Ensure **both devices** granted all permissions
3. Try these steps:
   - On Device A: Close and reopen the "Send Files" screen
   - On Device B: Close and reopen the "Receive Files" screen
   - Both devices should be discovering simultaneously

4. Check that Bluetooth is enabled on both devices:
   - Go to Settings → Bluetooth → Turn ON

5. Check that Location is enabled (required for device discovery):
   - Go to Settings → Location → Turn ON

6. Keep devices close together (within 10 meters)

#### Problem: "Transfer failed"

**Solutions:**
1. Check that both devices are still connected
2. Make sure the devices didn't go to sleep during transfer
3. Retry the transfer:
   - Go back to the main screen
   - Start the process again

#### Problem: "Permission denied"

**Solutions:**
1. Go to phone Settings → Apps → BlueShare → Permissions
2. Grant ALL permissions:
   - Location (Allow all the time or While using the app)
   - Nearby devices (Allow)
   - Bluetooth (Allow)
   - Photos and videos (Allow)
   - Music and audio (Allow)
   - Notifications (Allow)
3. Close and reopen the app

#### Problem: Devices discover each other but won't connect

**Solutions:**
1. Make sure Location is enabled on both devices
2. Try using Bluetooth instead of Wi-Fi Direct:
   - The app auto-selects the best method
   - Bluetooth is more reliable on some devices
3. Restart the app on both devices
4. Make sure no other apps are using Bluetooth/Wi-Fi Direct

---

### Tips for Best Performance

1. **Keep devices close together**
   - Ideal distance: 1-5 meters
   - Maximum: 10 meters for Wi-Fi Direct, 5 meters for Bluetooth

2. **Use Wi-Fi Direct for large files**
   - The app automatically prefers Wi-Fi Direct
   - Much faster than Bluetooth (up to 20-30 MB/s)

3. **Avoid interruptions**
   - Keep the app open during transfer
   - Don't let the screen turn off
   - Don't switch to other apps

4. **Grant all permissions**
   - The app needs all permissions to function properly
   - Location is required for device discovery (Android requirement)

5. **Keep devices charged**
   - File transfers can use battery
   - Connect to power if transferring large files

---

### Understanding the UI

#### Main Screen
- **Send Files** button → Opens sender mode
- **Receive Files** button → Opens receiver mode
- **Settings** (⚙️) → Coming soon

#### Transfer Screen (Send Mode)
- **Select Files** button → Opens file picker
- **Device list** → Shows discovered devices
- **Status text** → Shows current operation
- **Progress bar** → Shows transfer progress
- **Start Transfer** button → Begins sending (after connecting)
- **Cancel** button → Stops the transfer

#### Transfer Screen (Receive Mode)
- **Device list** → Shows discovered devices (receiver is discoverable)
- **Status text** → Shows "Waiting for sender..."
- **Progress bar** → Shows transfer progress when receiving
- Files automatically save to Downloads folder

---

### Technical Details

**Supported Transfer Methods:**
1. **Wi-Fi Direct** (Primary)
   - Speed: Up to 20-30 MB/s
   - Range: ~10 meters
   - Requires Android 4.0+

2. **Bluetooth Classic** (Fallback)
   - Speed: Up to 1-2 MB/s
   - Range: ~5 meters
   - More compatible with older devices

**File Types Supported:**
- Images (JPG, PNG, GIF, etc.)
- Videos (MP4, AVI, MKV, etc.)
- Documents (PDF, DOC, TXT, etc.)
- Audio (MP3, WAV, FLAC, etc.)
- APKs and ZIP files
- Any other file type

**Maximum File Size:**
- Limited only by device storage
- Recommended: Up to 2GB per transfer for best performance

---

### What's Next?

The current version (v0.2) includes the complete UI and device discovery. The actual file transfer over network sockets is the next feature to be implemented.

**Coming in Future Updates:**
- ✨ Real file transfer over Wi-Fi Direct and Bluetooth sockets
- ✨ Pause and resume transfers
- ✨ Transfer history
- ✨ Background transfers (continue even if app is minimized)
- ✨ QR code pairing for easier connection
- ✨ Group transfers (send to multiple devices)
- ✨ File encryption for security

---

### Need Help?

If you encounter issues not covered in this guide:
1. Check that you're using the latest version
2. Review the troubleshooting section
3. Make sure all permissions are granted
4. Try restarting both devices

---

**Version:** 0.2
**Last Updated:** 2025-09-30