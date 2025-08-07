# 🧬 InSituPy-QuPath

This repository contains a collection of scripts and documentation designed to connect [InSituPy](https://github.com/SpatialPathology/InSituPy) with [QuPath](https://qupath.github.io).

The scripts serve various purposes such as data export, preprocessing, and integration.

---

## 📂 Folder Overview

### `scripts/`
Scripts written in Groovy for use with [QuPath](https://qupath.github.io). These scripts help export data in formats compatible with InSituPy.

Included scripts:
- [`run_instanseg`](./scripts/run_instanseg.groovy) Runs InstanSeg on all annotations of the current image. Annotations need to be uniquely named. Can be also combined with "Run for project" to apply it to multiple images within the same project. To successfully run the script, the [QuPath InstanSeg extension](https://github.com/qupath/qupath-extension-instanseg) has to be installed and run once via the user interface to download PyTorch. Also the models can be downloaded via the user interface.
- [`export_for_insitupy.groovy`](./scripts/export_for_insitupy.groovy): Exports multiplexed IF data including cellular measurements, images as well as nuclear and cellular boundaries. Requires segmentation of cells using e.g. [InstanSeg](https://github.com/instanseg/instanseg). Exported data can be read using `read_qupath` or `read_qupath_project`.
- [`export_images.groovy`](./scripts/export_images.groovy): Exports OME-TIFF images based on annotations.
- [`export_annotations`](./scripts/export_annotations.groovy): Exports annotations as GEOJSON files.

## 🚀 Getting Started

To make the scripts easily accessible within QuPath:

### 📁 Recommended Setup

Download or clone this repository into the following location depending on your operating system:

- **Windows:** `C:/Users/<your-username>/QuPath/scripts/InSituPy-QuPath`

- **macOS:** `/Users/<your-username>/QuPath/scripts/InSituPy-QuPath`

- **Linux:** `/home/<your-username>/QuPath/scripts/InSituPy-QuPath`

Replace `<your-username>` with your actual system username.

### ▶️ Accessing Scripts in QuPath

1. Open **QuPath**.
2. Go to: `Automate → Shared scripts...`
3. You will see the scripts listed and ready to run from within the QuPath interface.

This setup ensures the scripts are available across all QuPath projects and simplifies workflow integration.

To run the scripts on a whole QuPath project, one can use `Run → Run for project` in the script editor.

---

## 📄 License

This repository is licensed under the MIT License. See the `LICENSE` file for details.

---

## 🤝 Contributing

Contributions are welcome! If you have a useful script or improvement, feel free to open a pull request or issue.
