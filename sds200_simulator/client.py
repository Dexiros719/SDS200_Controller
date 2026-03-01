import socket

SCANNER_IP = "127.0.0.1"
SCANNER_PORT = 50536

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.settimeout(2)

print("UDP klient připraven. Zadej příkazy (např. MDL). Napiš 'exit' pro ukončení.\n")

while True:
    command = input("Zadej příkaz: ").strip()

    if command.lower() == "exit":
        print("Ukončuji klienta.")
        break

    if not command:
        continue

    if not command.endswith("\r"):
        command += "\r"

    sock.sendto(command.encode('utf-8'), (SCANNER_IP, SCANNER_PORT))

    try:
        response, _ = sock.recvfrom(1024)
        file = open('log.txt', 'w')
        file.write(response.decode('utf-8'))
        file.close()
        print("Odpověď zaznamenána do logu\n")
    except socket.timeout:
        print("Skener neodpověděl (timeout).\n")
