#!/usr/bin/bash
sudo apt update
sudo apt install python3-pip -y
pip3 install -r requirements.txt
sudo cp ./webhook.service /etc/systemd/system/webhook.service
sudo systemctl daemon-reload
sudo systemctl start webhook.service
sudo systemctl enable webhook.service