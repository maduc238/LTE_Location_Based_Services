Các gói dependence:
```
sudo apt updata
sudo apt install vim git
apt-get install libpcre3 libpcre3-dbg libpcre3-dev build-essential libpcap-dev   \
                libnet1-dev libyaml-0-2 libyaml-dev pkg-config zlib1g zlib1g-dev \
                libcap-ng-dev libcap-ng0 make libmagic-dev         \
                libnss3-dev libgeoip-dev liblua5.1-dev libhiredis-dev libevent-dev \
                python-yaml rustc cargo
sudo apt install ...
```
cbindgen v0.24.3
```
cargo install --force cbindgen
```

Clone:
```
git clone https://github.com/OISF/suricata.git
```
```
cd suricata
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
