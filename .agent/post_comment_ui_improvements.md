# Post Detail & Comment UI Improvements

## Overview
Redesigned the post detail and comment sections to create a clear, readable discussion experience that emphasizes content over decoration.

## What's New

### 1. Post Detail Header (`post_detail_header.xml`)
**Purpose**: Optimized layout for the main post at the top of the detail view

**Key Features**:
- **Larger author avatar** (36dp) - more prominent than comment avatars
- **Dominant title** (22sp, bold) - clearly the most important element
- **Extended reading optimization**:
  - Body text: 15sp with 6dp line spacing
  - High contrast text (#1A1A1A for title, #333333 for body)
  - Generous padding (20dp horizontal)
- **Separated author info**: Vertical layout shows name and timestamp stacked

### 2. Comment Section Header (`comment_section_header.xml`)
**Purpose**: Clear visual transition from post to comments

**Key Features**:
- **Gray divider bar** (8dp height, #F5F5F5) - creates obvious separation
- **Simple comment count** ("12 Comments") - functional, not decorative
- **Subtle bottom border** (1dp, #EEEEEE) - defines the boundary

### 3. Redesigned Comment Item (`comment_item.xml`)
**Purpose**: Thread-aware discussion layout

**Key Features**:

#### Visual Hierarchy
- **Smaller avatars** (20dp vs 36dp for post author) - maintains focus on text
- **Horizontal layout** - supports indentation for nested replies
- **Thread line support** - vertical line shows reply relationships
- **Indentation spacer** - programmatically adjustable for reply depth

#### OP Identification
- **Subtle "OP" badge** - small (10sp), light blue background (#E3F2FD)
- **Shown only when needed** - visibility controlled programmatically
- **No loud markers** - maintains clean aesthetic

#### Reply Context
- **Optional reply mention** - "Replying to @username" in muted color
- **Collapsible** - hidden when not needed
- **Blue username** (#2196F3) - clickable appearance

#### Text-First Design
- **Comment text**: 14sp with 4dp line spacing - comfortable reading
- **High contrast** (#333333) - legible without being harsh
- **Generous bottom margin** (10dp) - good breathing room

#### Minimal Interactions
- **Small icons** (20dp) - secondary to content
- **Muted tint** (#999999) - doesn't compete with text
- **Compact spacing** - upvote, downvote, reply all in one row
- **No backgrounds** - clean, flat design

### 4. Typography System

#### Post Detail
- **Title**: 22sp, Montserrat Bold, #1A1A1A, 4dp line spacing
- **Body**: 15sp, Montserrat Regular, #333333, 6dp line spacing
- **Author**: 14sp, Montserrat Semibold, #1A1A1A
- **Metadata**: 12sp, Montserrat Regular, #999999

#### Comments
- **Author**: 13sp, Montserrat Medium, #1A1A1A
- **Content**: 14sp, Montserrat Regular, #333333, 4dp line spacing
- **Metadata**: 12sp, Montserrat Regular, #999999
- **Counts**: 12sp, Montserrat Medium, #999999

## Color Palette

### Primary Text
- **High contrast**: #1A1A1A (titles, author names)
- **Body text**: #333333 (post/comment content)

### Secondary Text  
- **Metadata**: #999999 (timestamps, counts, button labels)
- **Separators**: #BBBBBB (dots, dividers)

### Accents
- **OP badge**: #2196F3 (text), #E3F2FD (background)
- **Links/mentions**: #2196F3

### Backgrounds
- **Divider**: #F5F5F5 (comment section separator)
- **Border**: #EEEEEE (subtle lines)
- **Thread line**: #E0E0E0 (reply connections)

## Implementation Notes

### For ViewHolders
1. **Post detail** should use `post_detail_header.xml` (not `post_item.xml`)
2. **Comment section header** should be inserted before first comment
3. **Comment items** need programmatic control for:
   - `indentationSpace` width (e.g., replyLevel * 24dp)
   - `threadLine` visibility (visible for replies)
   - `opBadge` visibility (visible when commenter == post author)
   - `replyContextContainer` visibility (visible when replying to someone)

### Spacing Guidelines
- **Post padding**: 20dp horizontal, 16dp top, 20dp bottom
- **Comment padding**: 20dp horizontal, 12dp vertical
- **Reply indentation**: 24dp per level (max 2-3 levels recommended)
- **Section separator**: 8dp height

## Design Philosophy

1. **Content is king** - Text should always be the primary focus
2. **Clear hierarchy** - Visual weight decreases from post → comments → interactions
3. **Comfortable reading** - Line spacing and sizing optimized for extended discussions
4. **Subtle distinctions** - OP and reply indicators present but not loud
5. **Breathing room** - Generous spacing prevents density fatigue

## Comparison: Before vs After

### Before
- Same avatar size for posts and comments
- No visual separation between post and comments
- Loud "ORIGINAL POSTER" label
- Complex reply indication with multiple text elements
- Pill-style backgrounds on all buttons
- Smaller text with tight spacing

### After
- Clear size hierarchy: Post author (36dp) > Comment author (20dp)
- Gray divider bar creates obvious transition
- Subtle "OP" badge only when needed
- Clean "Replying to @username" format
- Minimal flat buttons that stay secondary
- Optimized typography for comfortable reading
