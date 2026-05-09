# 发布到 GitHub Pages

这个目录已经是 GitHub Pages 静态站点根目录：

- `index.html`：作品集首页
- `assets/`：图片资源
- `.nojekyll`：禁用 Jekyll 处理，按静态文件原样发布
- `README.md`：说明文档

## 1. 新建 GitHub 仓库

在 GitHub 新建一个公开仓库，例如：

```text
portfolio
```

如果仓库名是 `portfolio`，发布后的默认地址通常是：

```text
https://<你的GitHub用户名>.github.io/portfolio/
```

## 2. 推送代码

在本目录执行：

```powershell
git init
git add .
git commit -m "init portfolio site"
git branch -M main
git remote add origin https://github.com/<你的GitHub用户名>/<你的仓库名>.git
git push -u origin main
```

如果使用 SSH：

```powershell
git remote add origin git@github.com:<你的GitHub用户名>/<你的仓库名>.git
git push -u origin main
```

## 3. 开启 GitHub Pages

进入仓库页面：

1. 打开 `Settings`
2. 点击左侧 `Pages`
3. `Build and deployment` 选择 `Deploy from a branch`
4. Branch 选择 `main`
5. Folder 选择 `/ (root)`
6. 点击 `Save`

等待 1-3 分钟后，页面会显示发布地址。

## 4. 注意事项

- 不能只上传 `index.html`，必须保留 `assets/` 目录。
- 如果页面打开后图片不显示，检查仓库根目录是否包含 `assets` 文件夹。
- 如果 GitHub Pages 还没生效，等待几分钟后刷新 Pages 设置页。
