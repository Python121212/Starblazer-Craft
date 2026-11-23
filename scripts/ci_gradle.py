"""Run Gradle and expose actionable failures in GitHub check annotations too."""
import subprocess
import sys

result = subprocess.run(['gradle', '--no-daemon', 'build'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
print(result.stdout)
if result.returncode:
    tail = '\n'.join(result.stdout.splitlines()[-80:])
    escaped = tail.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print('::error title=Gradle build failed::' + escaped)
sys.exit(result.returncode)
