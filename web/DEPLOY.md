# Deploy And Connect A Domain

This app is a static website. The recommended setup is:

1. Push this repository to GitHub.
2. Deploy the `web` folder to a static host.
3. Connect your domain in the host dashboard.
4. Add the DNS records requested by the host.

## Recommended: Cloudflare Pages

Good fit if you want a custom domain, HTTPS, and no server maintenance.

Cloudflare Pages settings:

- Framework preset: `None`
- Build command: leave empty
- Build output directory: `web`
- Root directory: repository root

Domain setup:

- Add the custom domain in Cloudflare Pages.
- If the domain is already managed by Cloudflare DNS, Cloudflare can create the needed DNS record.
- For `www.example.com`, use the CNAME target Cloudflare Pages gives you.
- For `example.com`, Cloudflare must manage the zone for the apex domain.

## Alternative: Vercel

Vercel settings:

- Framework preset: `Other`
- Build command: leave empty
- Output directory: `web`

Domain setup:

- Add the domain in the Vercel project.
- For a subdomain like `www.example.com`, create the CNAME record Vercel shows.
- For an apex domain like `example.com`, use Vercel's required A/ALIAS-style DNS setup shown in the dashboard.

## Alternative: GitHub Pages

Use this if the repository is already on GitHub and you want the simplest free static hosting path.

Repository settings:

- Pages source: GitHub Actions, or deploy from a branch that contains only the `web` folder content.
- Custom domain: enter your domain in GitHub Pages settings.
- Enable HTTPS after DNS validation succeeds.

For a custom domain, GitHub Pages normally expects:

- `www.example.com`: CNAME to your GitHub Pages host.
- `example.com`: A/AAAA records to GitHub Pages IPs.

## Not Recommended: Open This PC Directly

You can expose the local Node server, but it is usually worse for this app:

- You must keep this PC always on.
- You must configure router port forwarding and Windows Firewall.
- You need HTTPS termination.
- If your home IP changes, you need dynamic DNS.

If you still need a temporary public preview from this PC, use a tunnel service such as Cloudflare Tunnel instead of opening router ports.

## Before Launch

- Decide canonical domain: `example.com` or `www.example.com`.
- Redirect the other variant to the canonical one.
- Verify HTTPS is enabled.
- Test these paths:
  - `/`
  - `/assets/content-pack.json`
  - `/assets/icon.webp`
