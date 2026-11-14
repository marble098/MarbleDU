# Operator logos

The files in `app/src/main/res/drawable-nodpi/op_*.webp` are the logos of Iranian
mobile operators and ISPs. They are **trademarks of their respective owners** and are
shown in MarbleDU only to identify the operator a connection belongs to. MarbleDU is
not affiliated with or endorsed by any of these companies. If you are a rights holder
and want a logo removed or replaced, please open an issue.

## Sources

| Logos | Source |
|---|---|
| MCI (Hamrah-e Aval), Irancell, RighTel, Taliya, ApTel, SamanTel, LotusTel, TeleKish, MTCE (Spadan) | [`MohammadTaghipour/pars_validator`](https://github.com/MohammadTaghipour/pars_validator) – `assets/icons/operators`, rasterised from the SVG sources (logos remain trademarks of their owners) |
| Shatel (also used for Shatel Mobile), Asiatech, HiWEB, Pars Online, MobinNet, SabaNet, Pishgaman, Zi-Tel | [`persianlabs/icons`](https://github.com/persianlabs/icons) – rasterised from the SVG sources |
| TCI, Fanava, Laser, Mabna Telecom, Datak, Afranet, MahanNet, Respina, TIC | Publicly published logo images found via web image search (operator sites, Wikipedia, logo directories) |

**Preparation (v۱.۶.۴):** all logos went through one pipeline so they look like a set —
transparent background, empty margins trimmed, the mark fitted into a square 256×256
canvas with equal padding on all four sides. Nothing is cropped, stretched or
recoloured; the app only picks a display scale per logo (`OperatorLogos.fitScale`) so
wide wordmarks do not look smaller than square emblems.

A rendered contact sheet of all bundled logos (white tile on the app's dark surface)
lives at `branding/operator-logos-preview.png`.

Anarestan and Petiak have no bundled logo yet (no trustworthy source was found); the
app falls back to the coloured letter badge for them and for any unknown ISP.
