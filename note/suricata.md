Các gói dependence:
```
sudo apt update
sudo apt install vim git
sudo apt-get install libpcre3 libpcre3-dbg libpcre3-dev build-essential libpcap-dev   \
                libnet1-dev libyaml-0-2 libyaml-dev pkg-config zlib1g zlib1g-dev \
                libcap-ng-dev libcap-ng0 make libmagic-dev         \
                libnss3-dev libgeoip-dev liblua5.1-dev libhiredis-dev libevent-dev \
                python-yaml rustc
sudo apt-get install cargo
sudo apt install libpcre2-dev libjansson-dev
```
cbindgen v0.24.3
```
cargo install --force cbindgen
```
Chỉnh bash:
```
vim ~/.bashrc
```
Thêm:
```
export PATH="/home/duc/.cargo/bin:$PATH"
```

Clone:
```
git clone https://github.com/OISF/suricata.git
```
```
cd suricata
git clone https://github.com/OISF/libhtp
```
```
./autogen.sh
```
```
./configure --enable-nfqueue --prefix=/usr --sysconfdir=/etc --localstatedir=/var
make
make install
```
Thêm protocol:
```
scripts/setup-app-layer.py Diameter
```
Lệnh sysctl:
```
systemctl enable suricata.service 
systemctl status suricata.service
systemctl restart suricata
```
