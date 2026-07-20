#!/usr/bin/env python3
import json
import os
import sys
import zipfile
from pathlib import Path

GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = '\033[1;33m'
NC = '\033[0m'

def main():
    libs_dir = Path('build/libs')
    jars = [f for f in libs_dir.glob('*.jar') if 'sources' not in f.name]
    if not jars:
        print(f"{RED}ERROR: No JAR file found in build/libs/{NC}")
        print("Run './gradlew build' first")
        return 1

    jar_file = jars[0]
    print(f"Validating JAR: {jar_file.name}\n")

    errors = 0
    total_mixins = 0

    for json_file in Path('src/main/resources').glob('*.mixins.json'):
        config_name = json_file.name
        print(f"Checking {config_name}...")

        with open(json_file) as f:
            config = json.load(f)

        package = config.get('package', '')
        if not package:
            print(f"  {RED}[FAIL] Missing package declaration{NC}")
            errors += 1
            continue

        for array_type in ['mixins', 'client', 'server']:
            mixins = config.get(array_type, [])
            if not mixins:
                continue

            print(f"  Validating .{array_type}[]...")
            for mixin in mixins:
                total_mixins += 1
                class_path = package.replace('.', '/') + '/' + mixin.replace('.', '/') + '.class'

                with zipfile.ZipFile(jar_file, 'r') as zf:
                    found = any(class_path in name for name in zf.namelist())

                if found:
                    print(f"    {GREEN}[OK]{NC} {mixin}")
                else:
                    print(f"    {RED}[FAIL] {mixin}{NC}")
                    print(f"      {YELLOW}Expected: {class_path}{NC}")
                    print(f"      {YELLOW}Status: NOT FOUND IN JAR{NC}")
                    errors += 1

        print()

    print("=== VALIDATION SUMMARY ===")
    print(f"Total mixins checked: {total_mixins}")
    print(f"JAR file: {jar_file.name}")
    print(f"JAR size: {jar_file.stat().st_size} bytes\n")

    if errors == 0:
        print(f"{GREEN}[OK] ALL CHECKS PASSED{NC}")
        print("All mixin references are valid and present in JAR")
        return 0
    else:
        print(f"{RED}[FAIL] VALIDATION FAILED{NC}")
        print(f"Found {errors} phantom mixin reference(s)\n")
        print("These mixins are declared in JSON configs but missing from the JAR.")
        print("This will cause ClassNotFoundException at runtime.\n")
        print("Common causes:")
        print("  1. Mixin class excluded from compilation (check build.gradle sourceSets)")
        print("  2. Mixin class deleted but not removed from JSON")
        print("  3. Typo in mixin class name")
        return 1

if __name__ == '__main__':
    sys.exit(main())
