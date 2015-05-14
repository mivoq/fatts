Source: marytts_5.2~20150504125100.orig.tar.gz
Summary: text-to-speech synthesis server
Name: marytts
Version: 5.2~20150504125100
Release: 1
Group: Applications/Sound
License: see /usr/share/doc/marytts/copyright
Distribution: RedHat
Packager: Giulio Paci <giulio.paci@mivoq.it>
BuildArch: noarch
Requires: java >= 1.7.0

%description
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package includes the MaryTTS RESTFUL server.

%define _rpmdir ../
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm
%define _unpackaged_files_terminate_build 0
%define _up_version 5.2-SNAPSHOT

%prep
rm -fr marytts-%{version}
mkdir -p marytts-%{version}
cd marytts-%{version}
zcat $RPM_SOURCE_DIR/marytts_%{version}.orig.tar.gz |  tar -xvf - 

%build
cd $RPM_BUILD_DIR/marytts-%{version}
mvn install

%install
cd $RPM_BUILD_DIR/marytts-%{version}
install -m 755 -d %{buildroot}/usr/share/marytts/%{_up_version}/bin
install -m 755 -d %{buildroot}/usr/share/marytts/%{_up_version}/lib
install -m 755 -d %{buildroot}%{_bindir}
install -m 755 -d %{buildroot}/etc/default/
install -m 755 -d %{buildroot}/etc/marytts/
install -m 755 -d %{buildroot}/etc/init.d/
install -m 755 -d %{buildroot}/lib/systemd/system/
install -m 755 target/marytts-%{_up_version}/bin/* %{buildroot}/usr/share/marytts/%{_up_version}/bin/
install -m 644 target/marytts-%{_up_version}/lib/* %{buildroot}/usr/share/marytts/%{_up_version}/lib/
install -m 644 target/marytts-%{_up_version}/doc/examples/etc/default/marytts-server %{buildroot}/etc/default/
install -m 644 target/marytts-%{_up_version}/doc/examples/etc/marytts/log4j.properties %{buildroot}/etc/marytts/
install -m 755 target/marytts-%{_up_version}/doc/examples/etc/init.d/marytts-server %{buildroot}/etc/init.d/
install -m 644 target/marytts-%{_up_version}/doc/examples/lib/systemd/system/marytts-server.service %{buildroot}/lib/systemd/system/
ln -sf /usr/share/marytts/%{_up_version}/bin/marytts-server %{buildroot}%{_bindir}

%files
/usr/share/marytts/%{_up_version}/bin/marytts-server
/usr/share/marytts/%{_up_version}/lib/marytts-runtime-5.2-SNAPSHOT-jar-with-dependencies.jar
/etc/default/marytts-server
/etc/marytts/log4j.properties
/etc/init.d/marytts-server
/lib/systemd/system/marytts-server.service
%{_bindir}/marytts-server

%clean
cd $RPM_BUILD_DIR/marytts-%{version}
mvn clean

%pre
#!/bin/sh

set -e;

if ! getent group marytts >/dev/null; then
        # Adding system group: marytts.
        groupadd -r -f marytts >/dev/null
fi

USER_SHELL=/bin/false
if [ -x /sbin/nologin ] ; then
    USER_SHELL=/sbin/nologin
fi

# creating marytts user if he isn't already there
if ! getent passwd marytts >/dev/null; then
        # Adding system user: marytts.
        useradd \
          -r \
          -g marytts \
	  -M \
          -d /nonexistent \
          -c "MaryTTS Server" \
          -s $USER_SHELL \
          marytts  >/dev/null
fi

LOG_READER_GROUP=adm
if ! getent group "$LOG_READER_GROUP" >/dev/null; then
    LOG_READER_GROUP=marytts
fi

if [ ! -d /var/log/marytts ]; then
        mkdir /var/log/marytts
        touch /var/log/marytts/server.log
	chown -R marytts:"$LOG_READER_GROUP" /var/log/marytts
        chmod 640 /var/log/marytts/server.log
        chmod 750 /var/log/marytts/
fi

exit 0



%package voice-en-cmu-slt-hsmm
Summary: English speaker for MaryTTS
Group: Applications/Sound
Requires: marytts-lang-en
%description voice-en-cmu-slt-hsmm
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the speaker modules required to synthesize speech
in English.

%files voice-en-cmu-slt-hsmm
/usr/share/marytts/%{_up_version}/lib/voice-cmu-slt-hsmm-%{_up_version}.jar

################################################################################

%package lang-de
Summary: marytts modules for German language
Group: Applications/Sound
Requires: marytts
%description lang-de
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
German.

%files lang-de
/usr/share/marytts/%{_up_version}/lib/marytts-lang-de-%{_up_version}.jar

%package lang-en
Summary: marytts modules for English language
Group: Applications/Sound
Requires: marytts
%description lang-en
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
English.

%files lang-en
/usr/share/marytts/%{_up_version}/lib/marytts-lang-en-%{_up_version}.jar

%package lang-fr
Summary: marytts modules for French language
Group: Applications/Sound
Requires: marytts
%description lang-fr
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
French.

%files lang-fr
/usr/share/marytts/%{_up_version}/lib/marytts-lang-fr-%{_up_version}.jar

%package lang-it
Summary: marytts modules for Italian language
Group: Applications/Sound
Requires: marytts
%description lang-it
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
Italian.

%files lang-it
/usr/share/marytts/%{_up_version}/lib/marytts-lang-it-%{_up_version}.jar

%package lang-ru
Summary: marytts modules for Russian language
Group: Applications/Sound
Requires: marytts
%description lang-ru
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
Russian.

%files lang-ru
/usr/share/marytts/%{_up_version}/lib/marytts-lang-ru-%{_up_version}.jar

%package lang-sv
Summary: marytts modules for Swedish language
Group: Applications/Sound
Requires: marytts
%description lang-sv
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
Swedish.

%files lang-sv
/usr/share/marytts/%{_up_version}/lib/marytts-lang-sv-%{_up_version}.jar

%package lang-te
Summary: marytts modules for Telugu language
Group: Applications/Sound
Requires: marytts
%description lang-te
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
Telugu.

%files lang-te
/usr/share/marytts/%{_up_version}/lib/marytts-lang-te-%{_up_version}.jar

%package lang-tr
Summary: marytts modules for Turkish language
Group: Applications/Sound
Requires: marytts
%description lang-tr
MaryTTS is an open-source, multilingual Text-to-Speech Synthesis
platform written in Java.

This package provides the modules required to synthesize speech in
Turkish.

%files lang-tr
/usr/share/marytts/%{_up_version}/lib/marytts-lang-tr-%{_up_version}.jar

