import DashboardLayout from '../pages/Layout/DashboardLayout.vue'
import WindowView from "../pages/WindowView";

const routes = [
    {
        path: '/',
        component: DashboardLayout,
        redirect: '/window-view',
        children: [
            {
                name: 'window-view',
                path: 'window-view',
                component: WindowView
            },
            {
                name: 'window-view2',
                path: 'window-view2',
                component: WindowView
            }
        ]
    }
]

export default routes
