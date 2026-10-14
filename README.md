# PhotoCraft Web on GitHub Pages

This repository deploys the **official prebuilt PhotoCraft Web release** to GitHub Pages. It does not fork, patch, or modify the upstream project.

- Upstream project: https://github.com/storytold/photocraft
- Official web hosting notes: https://github.com/storytold/photocraft/blob/main/packaging/web/README.md
- Release currently pinned by the workflow: [PhotoCraft v0.8.2](https://github.com/storytold/photocraft/releases/tag/v0.8.2)
- Release asset: `photocraft-web-0.8.2.zip`

## Deployment

A GitHub Actions workflow downloads the official release ZIP, locates `index.html`, copies the static site into a Pages artifact, and deploys it. A push to `main` or a manual workflow run triggers deployment.

To update PhotoCraft later, change the release tag and ZIP filename in `.github/workflows/deploy.yml` after verifying the corresponding official release asset exists.

## Notes

- PhotoCraft runs image editing in the browser; this repository does not add a server or upload users' working images.
- Use the browser's Open, Save, and Export functions. Save/download your work before refreshing or closing the page.
- Browser support depends on WebGPU or WebGL2 availability. If rendering fails, try adding `?webgl` to the site URL.
- PhotoCraft's source code and release are maintained by the upstream `storytold/photocraft` project. Refer to its repository for the applicable licenses and notices.
