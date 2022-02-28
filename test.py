import pexpect

# pip install pexpect
client = pexpect.spawn('java -jar ChatClient.jar  --host localhost --port 9991 -i "Alice"')
client.sendline("mypassword")
client.expect("defc")

if __name__ == '__main__':
    pass
