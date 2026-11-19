#!/usr/bin/env python3
"""Pack an avatar that cannot be redistributed into a phone-importable zip.

Some of the avatars used during development are demo material whose terms do
not allow us to publish them in the public preset repository. Users can still
keep them on their own phone: this tool writes a zip that the app imports
through Avatar Configuration -> the Import Model button (the picker accepts
application/zip), and the zip is never committed here.

Layout inside the zip, which is what AvatarRepository.importAvatarFromZip
expects (it scans every directory for a model the glTF delegate understands):

    <Name>/model.glb
    <Name>/LICENSE.txt
    <Name>/README.txt

Usage:
    python3 pack_local_bundle.py --name Xbot --model /path/model.glb \
        --out-dir /home/user/bundles \
        --origin https://github.com/mrdoob/three.js/tree/dev/examples/models/gltf/Xbot.glb \
        --license "three.js examples bundle, Mixamo export; terms need review" \
        [--extra /path/thumbnail.png]
"""

import argparse
import os
import zipfile

README = """{name}

Origin: {origin}
Licence: {license}

This archive is not part of the public avatar preset repository because the
terms do not allow redistribution from there. It is meant for your own phone.

How to install:
1. Copy {name}.zip to the phone.
2. Open Assistant Config -> Avatar Configuration.
3. Tap the Import Model button next to Current Model and pick the zip.
4. Pick {name} in the Current Model list.
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", required=True)
    ap.add_argument("--model", required=True)
    ap.add_argument("--out-dir", required=True)
    ap.add_argument("--origin", required=True)
    ap.add_argument("--license", required=True)
    ap.add_argument("--extra", action="append", default=[])
    args = ap.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    out = os.path.join(args.out_dir, args.name + ".zip")
    readme = README.format(name=args.name, origin=args.origin, license=args.license)
    members = {
        f"{args.name}/LICENSE.txt": args.license + "\n\nOrigin: " + args.origin + "\n",
        f"{args.name}/README.txt": readme,
    }
    for extra in args.extra:
        with open(extra, "rb") as fh:
            members[f"{args.name}/{os.path.basename(extra)}"] = fh.read()
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(args.model, f"{args.name}/model.glb")
        for name, data in sorted(members.items()):
            z.writestr(name, data)
    print(out, os.path.getsize(out), "bytes")


if __name__ == "__main__":
    main()
