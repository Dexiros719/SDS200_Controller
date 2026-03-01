import socket
import time


def generate_gsi_xml():
    # toggle system name and maybe frequency every 5 seconds
    t = int(time.time() // 2)
    if t % 2 == 0:
        name = "Calcasieu"
        freq = "154.4150MHz"
    else:
        name = "OtherSystem"
        freq = "155.5250MHz"

    xml = ("GSI,<?xml version=\"1.0\" encoding=\"utf-8\"?>"
           "<ScannerInfo Mode=\"Trunk Scan Hold\" V_Screen=\"trunk_scan\">"
           "<MonitorList Name=\"Full Database\" Index=\"4294967295\" ListType=\"FullDb\" Q_Key=\"None\" N_Tag=\"None\" DB_Counter=\"3\" />"
           f"<System Name=\"{name}\" Index=\"283\" Avoid=\"Off\" SystemType=\"Conventional\" Q_Key=\"None\" N_Tag=\"None\" Hold=\"On\" />"
           "<Department Name=\"Calcasieu Parish - Parish Fire &amp; Medical\" Index=\"286\" Avoid=\"Off\" Q_Key=\"None\" Hold=\"Off\" />"
           f"<ConvFrequency Name=\"DeQuincy Fire Department\" Index=\"290\" Avoid=\"Off\" Freq=\" {freq}\"Mod=\"NFM\" N_Tag=\"None\" Hold=\"On\" SvcType=\"Fire Dispatch\" P_Ch=\"Off\" SAS=\"All\" SAD=\"None\" LVL=\"0\" IFX=\"Off\" />"
           "<AGC A_AGC=\"Off\" D_AGC=\"Off\" />"
           "<DualWatch PRI=\"Off\" CC=\"Off\" WX=\"Off\" />"
           "<PropertyVOL=\"0\" SQL=\"9\" Sig=\"0\"WiFi=\"3\" Att=\"Off\" Rec=\"Off\"KeyLock=\"Off\" P25Status=\"None\"Mute=\"Mute\" Backlight=\"100\"Rssi=\"0.377\"/>"
           "<ViewDescription><InfoArea1 Text=\"F0:01234-6*789\" /><InfoArea2 Text=\"S3:01234-6*---\" /><PopupScreen Text=\"Quick Save?\n\"/></ViewDescription>"
           "</ScannerInfo>")
    return xml

UDP_IP = "0.0.0.0"
UDP_PORT = 50536

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
# allow immediate reuse of the address when restarting the script
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind((UDP_IP, UDP_PORT))

print(f"Skener naslouchá na {UDP_IP}:{UDP_PORT}")

def handle_command(command):
    if command == "MDL\r":
        return "MDL,SDS200\r"
    elif command == "VER\r":
        return "VER,Version 1.01.20\r"
    elif command == "QSH,1\r":
        return "QSH,OK\r"
    elif command == "KEY,0,1\r":
        return "KEY,OK\r"
    elif command == "KEY,1,1\r":
        return "KEY,OK\r"
    elif command == "KEY,2,1\r":
        return "KEY,OK\r"
    elif command == "KEY,3,1\r":
        return "KEY,OK\r"
    elif command == "KEY,4,1\r":
        return "KEY,OK\r"
    elif command == "KEY,5,1\r":
        return "KEY,OK\r"
    elif command == "KEY,6,1\r":
        return "KEY,OK\r"
    elif command == "KEY,7,1\r":
        return "KEY,OK\r"
    elif command == "KEY,8,1\r":
        return "KEY,OK\r"
    elif command == "KEY,9,1\r":
        return "KEY,OK\r"
    elif command == "MSI\r":
        return 'MSI,<XML>,\r<?xml version="1.0" encoding="utf-8"?>\r<MSI Name=" Title " Index="xxxxxx ">\r</MSI>\r'
    elif command == "NXT,1,1,1,1\r":
        return "NXT,OK\r"
    elif command == "PRV,1,1,1,1\r":
        return "PRV,OK\r"
    elif command == "HLD,1,1,1\r":
        return "HLD,OK\r"
    elif command == "AVD,1,1,1 1\r": #V dokumentaci jsou čísla přímo za sebou, potřeba checknout formát
        return "AVD,OK\r"
    elif command == "GST\r":
        return 'GST,[DSP_FORM],[L1_CHAR],[L1_MODE],[L2_CHAR],[L2_MODE],[L3_CHAR],[L3_MODE],...,[L20_CHAR],[L20_MODE],[MUTE],[RSV],[RSV],[WF_MODE],[FREQ],[MOD],[MF_POS],[CF],[LOWER],[UPPER],[RSV],[FFT_SIZE]\r'
    elif command == "GSI\r":
        # return an XML string with a system name that toggles every 5 seconds
        return generate_gsi_xml()

    elif command == "ret\r":
        return quit()
    else:
        return "UNKNOWN COMMAND"

while True:
    data, addr = sock.recvfrom(1024)
    command = data.decode('utf-8')
    # only log the command name (before any comma) and the sender address
    cmd_name = command.strip().split(',')[0]
    print(f"Příkaz {cmd_name} od {addr}", flush=True)

    response = handle_command(command)
    # persist and send response as before
    with open('log.txt', 'w') as file:
        file.write(response)
    sock.sendto(response.encode('utf-8'), addr)
