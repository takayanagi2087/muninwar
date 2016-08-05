# munin.war[Munin用Tomcat plugin servlet].
Munin用のTomcat pluginです。

##特徴
* すでにperl等で作られたものはありますが、似たような機能をjava servletで実装しました。
* muninのpluginの機能は全てservletで実装してあります。
* あとはshell scriptからwget -q -O-を呼ぶだけです。

##インストール手順
* munin.warをhttp://woontai.dip.jp/muninwardownload/ からダウンロードする。
* munin.warをtomcatのwebappsにコピーする。
* http://localhost:8080/munin/をアクセスして、解説ページが表示されることを確認する。
* webapps/munin/shにあるcatalina_*をmuninのplugin directory(centosの場合/usr/share/munin/plugins)にコピーする。
* コピーしたcatalina_*に実行権限を与える。(chmod +x catalina_*)
* コピーしたpluginを所定のdirectory(centosの場合/etc/munin/plugins)にsymbolic linkするか、以下のcommandで自動追加する。
/usr/sbin/munin-node-configure --shell | grep catalina | sh
* munin-nodeを再起動(centosの場合はservice munin-node restart)して完了。

##更新履歴
* 1.01 tomcat8.0対応。tomcay9でも動作を確認(2016/08/05)。

##License
GPLv2
