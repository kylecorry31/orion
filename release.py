import os
import re
import subprocess

with open("build.gradle.kts", "r") as gradle:
    contents = gradle.read()

version_name = re.search('versionName = "(.+)"', contents).group(1)

script_dir = os.path.dirname(os.path.abspath(__file__))
commits_result = subprocess.run(
    ["./scripts/list-new-commits.sh"],
    capture_output=True,
    text=True,
    cwd=script_dir,
)
release_notes = commits_result.stdout.strip()

print("Creating draft release for version", version_name)

subprocess.run(
    [
        "gh",
        "release",
        "create",
        version_name,
        "-t",
        version_name,
        "-n",
        release_notes,
        "-d",
    ]
)
