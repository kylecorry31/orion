#!/bin/bash

latest_tag=$(git describe --tags --abbrev=0)
git log "$latest_tag"..HEAD --oneline --abbrev-commit
