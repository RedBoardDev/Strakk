import {
  Home, Calendar, BarChart3, Settings, Flame, Dumbbell, Droplet, Droplets,
  Leaf, Wheat, Plus, ScanBarcode, Camera, Search, X, ChevronRight, ChevronLeft,
  ChevronDown, Pencil, Trash2, Check, Sparkles, Clock, Minus, Egg,
  Bell, ChevronUp, Apple, Utensils, PenLine, Image as ImageIcon, TrendingUp,
  TrendingDown, Ruler, Scale, Heart, ArrowLeft, Info, LogOut, Globe, Star,
  MoreHorizontal, Share,
  type LucideProps,
} from 'lucide-react'

// SF-Symbol-flavored names → lucide. Keeps screens reading like the iOS source.
const MAP = {
  'house.fill': Home,
  calendar: Calendar,
  'chart.bar': BarChart3,
  'gearshape.fill': Settings,
  'flame.fill': Flame,
  'dumbbell.fill': Dumbbell,
  'drop.fill': Droplet,
  drops: Droplets,
  'leaf.fill': Leaf,
  wheat: Wheat,
  plus: Plus,
  barcode: ScanBarcode,
  camera: Camera,
  search: Search,
  xmark: X,
  'chevron.right': ChevronRight,
  'chevron.left': ChevronLeft,
  'chevron.down': ChevronDown,
  'chevron.up': ChevronUp,
  pencil: Pencil,
  trash: Trash2,
  check: Check,
  sparkles: Sparkles,
  clock: Clock,
  minus: Minus,
  egg: Egg,
  bell: Bell,
  apple: Apple,
  fork: Utensils,
  note: PenLine,
  photo: ImageIcon,
  'trend.up': TrendingUp,
  'trend.down': TrendingDown,
  ruler: Ruler,
  scale: Scale,
  heart: Heart,
  back: ArrowLeft,
  info: Info,
  logout: LogOut,
  globe: Globe,
  star: Star,
  ellipsis: MoreHorizontal,
  share: Share,
} as const

export type IconName = keyof typeof MAP

export function Icon({ name, size = 18, ...rest }: { name: IconName; size?: number } & LucideProps) {
  const Cmp = MAP[name]
  return <Cmp size={size} strokeWidth={2} {...rest} />
}
