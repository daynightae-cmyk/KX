# KX GLASS ORBIT — reference-locked aesthetic

Canonical logo: `assets/reference/00-brand-logo.png` at 1254×1254. Screens: 941×1672. These are **design references, not shippable flattened screens**.

- Core silhouette: circular illuminated pearl-glass orb, folded/ribbon shield emblem, three orbiting jewel nodes, lavender-violet/cyan sheen, soft diffraction.
- Suggested palette tokens (tune after visual pixel sampling): midnight #0B102C, ink #171B4C, violet #6537E8, orchid #BE69EF, cyan #5BCFFD, pearl #E7EFFF. Protect contrast rather than copying light text on pale glass.
- Two modes: near-black indigo night and pale lilac day. Preserve glossy waves/particles without obscuring UI.
- Material: layered translucent panels with rounded edges, gradient rim highlights, dynamic light reflection; avoid expensive bitmap blur in continuously scrolling lists.
- Layout: centered controlled-size brand emblem, generous breathing room, safe status/nav insets, compact functional hierarchy, accessible 48dp hits, typography EN/AR with adaptive line breaks.
- Motion: splash logo emerge/halo, valid connect/disconnect pulse, subtle orbital movement; disabled/reduced-motion support, no misleading state animations.
- Never substitute reference PNG as screen UI. Implement Semantics, real toggles, lists, chart touch/labels, loading/empty/error states, real navigation.

Visual QA: screenshot comparison at 360dp/411dp and 1.3× font, dark/day, EN/AR, Android accessibility. Record unavoidable differences and reviewed rationale.
