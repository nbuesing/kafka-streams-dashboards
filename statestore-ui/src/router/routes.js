import DashboardLayout from '../pages/Layout/DashboardLayout.vue'
import Tumbling from "../pages/Tumbling";
import Hopping from "../pages/Hopping";
import Sliding from "../pages/Sliding";
import Session from "../pages/Session";
import KeyStoreView from "../pages/KeyStoreView";

const routes = [
    {
        path: '/',
        component: DashboardLayout,
        redirect: '/tumbling',
        children: [
            {
                name: 'tumbling',
                path: 'tumbling',
                component: Tumbling
            },
            {
                name: 'hopping',
                path: 'hopping',
                component: Hopping
            },
            {
                name: 'sliding',
                path: 'sliding',
                component: Sliding
            },
            {
                name: 'session',
                path: 'session',
                component: Session
            },
            {
                name: 'no-windowing',
                path: 'no-windowing',
                component: KeyStoreView

            }
        ]
    }
]

export default routes
