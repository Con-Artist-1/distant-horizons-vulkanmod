import os
import sys
import urllib.request
import json
from pathlib import Path

def download_file(url, dest_path):
    print(f"Downloading {url} to {dest_path}...")
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(
        url, 
        headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
    )
    with urllib.request.urlopen(req) as response, open(dest_path, 'wb') as out_file:
        out_file.write(response.read())
    print("Download completed successfully.")

def get_modrinth_jar(slug, mc_ver):
    print(f"Searching Modrinth for {slug} (MC {mc_ver})...")
    url = f"https://api.modrinth.com/v2/project/{slug}/version?game_versions=%5B%22{mc_ver}%22%5D"
    req = urllib.request.Request(
        url,
        headers={'User-Agent': 'Mozilla/5.0'}
    )
    try:
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            if not data:
                print(f"No versions found for {slug} on MC {mc_ver}.")
                return None
            
            # Find the first version that has files (usually sorted newest first)
            for version in data:
                # We want loaders to include 'fabric'
                if 'fabric' in version.get('loaders', []):
                    for file in version.get('files', []):
                        if file.get('primary', False) or len(version.get('files', [])) == 1:
                            return file.get('url')
            
            # Fallback to any file in the first version
            if data[0].get('files'):
                return data[0]['files'][0].get('url')
    except Exception as e:
        print(f"Error querying Modrinth for {slug}: {e}")
    return None

def main():
    if len(sys.argv) < 2:
        print("Usage: python download_dependencies.py <mc_version>")
        sys.exit(1)
    
    mc_ver = sys.argv[1]
    
    dh_url = os.environ.get("DH_URL")
    vm_url = os.environ.get("VM_URL")
    
    jars_dir = Path("jars")
    jars_dir.mkdir(exist_ok=True)
    
    # 1. Distant Horizons
    if dh_url:
        print("Using provided DH direct URL.")
        download_file(dh_url, jars_dir / f"DistantHorizons-custom-{mc_ver}-fabric.jar")
    else:
        url = get_modrinth_jar("distant-horizons", mc_ver)
        if url:
            filename = f"DistantHorizons-modrinth-{mc_ver}-fabric.jar"
            download_file(url, jars_dir / filename)
        else:
            print("WARNING: Could not find Distant Horizons jar automatically.")
            print("Please upload or provide a direct URL to the Distant Horizons jar.")
            
    # 2. VulkanMod
    if vm_url:
        print("Using provided VulkanMod direct URL.")
        download_file(vm_url, jars_dir / f"VulkanMod_{mc_ver}-custom.jar")
    else:
        url = get_modrinth_jar("vulkanmod", mc_ver)
        if url:
            filename = f"VulkanMod_{mc_ver}-modrinth.jar"
            download_file(url, jars_dir / filename)
        else:
            print("WARNING: Could not find VulkanMod jar automatically.")
            print("Please upload or provide a direct URL to the VulkanMod jar.")

if __name__ == "__main__":
    main()
