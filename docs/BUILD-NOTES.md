# �������Ӽ�¼

���ĵ���¼�������ã�**��������֤�Ĺ����̳�**���û�Ҫ����ͣ���������β��������롣

| ��� | �����еİ汾/���� |
| --- | --- |
| Android Gradle Plugin | 9.3.0 |
| Kotlin / Compose ������ | 2.4.0 |
| ���س��Ե� Gradle | 9.5.0 |
| ���� JDK | 21 |
| Android API | compile/target 37��min 29 |
| NDK | 28.2.13676358 |
| CMake | 3.22.1 |
| Rust ����֤������ | 1.99.1 |
| Rust target | aarch64-linux-android |
| ABI | arm64-v8a |
| Compose BOM | 2026.09.00 |
| Material 3 | 1.5.0-beta01 |

`app/build.gradle.kts` ���� Rust ��������CMake �����侲̬�Ⲣ���� JNI ��̬�⡣�Ѽ��� 16 KB ҳ����������ã�����δ������ APK ��֤��

�ֿ�û�� Gradle Wrapper��Ҳ���� Android SDK��NDK��Rust ��װĿ¼�� Maven ���档`local.properties` �Ǳ��� SDK ·�����ѱ����ԡ�`NOCA_CARGO` ��ָ�� Cargo ·����`NOCA_MAVEN_MIRROR` ��Ϊ������������Ԥ���Ŀ�ѡ���� Maven �ֿ⡣

�����������Ͻű��浵�� `archive/tooling/`��`noca-build.ps1` �� `maven-fetch.mjs`������ԭ�������˽�ǰ�ڳ��ԣ����ǰ���ԭ�������ṹ�뱾��·������Ӧ������������Զ���װ�򹹽��������ű������õĹ������ͻ��治��ֿ�ַ���

���һ�� Android ��������� Compose ���/���������׶Ρ�����׼���˱����������񣬵�û����ɺ��� Android �������ա�Rust ���Լ� ARM64 ��̬��ĳɹ�������� JNI��Compose��APK ��ʵ�����ա�
