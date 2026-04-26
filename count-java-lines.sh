#!/bin/bash
echo "Counting lines in Java files (.java) for Spring Boot project..."
echo "Excluding: target, .gradle, build, node_modules, .git, etc."
echo "================================================================="

# Find all .java files and count lines (with nice formatting)
find . -type f -name "*.java" \
  ! -path "*/target/*" \
  ! -path "*/.gradle/*" \
  ! -path "*/build/*" \
  ! -path "*/node_modules/*" \
  ! -path "*/.git/*" \
  ! -path "*/.idea/*" \
  ! -path "*/.vscode/*" \
  ! -path "*/out/*" \
  | xargs wc -l | sort -nr

echo "================================================================="
echo "Total lines in all Java files:"
find . -type f -name "*.java" \
  ! -path "*/target/*" \
  ! -path "*/.gradle/*" \
  ! -path "*/build/*" \
  ! -path "*/node_modules/*" \
  ! -path "*/.git/*" \
  ! -path "*/.idea/*" \
  ! -path "*/.vscode/*" \
  ! -path "*/out/*" \
  | xargs wc -l | tail -1