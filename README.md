# 🧬 InSituPy-Scripts

This repository contains a collection of scripts designed to complement the use of InSituPy, a Python package for analyzing single-cell spatial transcriptomics and proteomics data.

The scripts are grouped by tool or language and serve various purposes such as data export, preprocessing, and integration.

---

## 📂 Folder Overview

### `qupath/`
Scripts written in Groovy for use with [QuPath](https://qupath.github.io). These scripts help export image data and annotations in formats compatible with InSituPy.

Included scripts:
- `ExportToInSituPy.groovy`: Exports multiplexed IF data including cellular measurements, images as well as nuclear and cellular boundaries. Requires segmentation of cells using e.g. [InstanSeg](https://github.com/instanseg/instanseg). Exported data can be read using `read_qupath` or `read_qupath_project`.
- `ExportOMETIFF.groovy`: Exports OME-TIFF images based on annotations.

Each script is documented in the folder's `README.md`.

---

## 📄 License

This repository is licensed under the MIT License. See the `LICENSE` file for details.

---

## 🤝 Contributing

Contributions are welcome! If you have a useful script or improvement, feel free to open a pull request or issue.
